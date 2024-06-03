package me.pood1e.jobstream.configjob.core.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.configjob.core.config.ExternalConfig;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class PluginClassLoaderUtils {

    private final static Map<String, PluginClassLoader> CLASS_LOADERS = new ConcurrentHashMap<>();
    private final static Map<String, String> JOB_MANAGER_PLUGIN_HASH = new HashMap<>();
    private final static Map<String, String> TASK_MANAGER_PLUGIN_HASH = new HashMap<>();
    private final static Map<String, String> CLASS_NAME_MAP = new ConcurrentHashMap<>();
    private final static Map<String, Map<String, Object>> CREATED_OBJECTS = new ConcurrentHashMap<>();
    private final static ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    public static List<String> getPluginClass(String pluginId) {
        return CLASS_NAME_MAP.entrySet().stream().filter(entry -> entry.getValue().equals(pluginId))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private static <T> Class<T> findOrLoadClass(String className) {
        try {
            LOCK.readLock().lock();
            String pluginId = findPluginIdByClass(className);
            PluginClassLoader classLoader = findPluginClassLoader(pluginId);
            return (Class<T>) classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    private static String findPluginIdByClass(String className) {
        try {
            LOCK.readLock().lock();
            String pluginId = CLASS_NAME_MAP.get(className);
            if (pluginId != null) {
                return pluginId;
            }
            Plugin plugin = RequestUtils.getPluginByClassName(className);
            plugin.getClasses().forEach(pluginClass -> CLASS_NAME_MAP.put(pluginClass.getClassName(), plugin.getId()));
            JOB_MANAGER_PLUGIN_HASH.put(plugin.getId(), plugin.getJarHash());
            return plugin.getId();
        } finally {
            LOCK.readLock().unlock();
        }
    }

    private static PluginClassLoader findPluginClassLoader(String pluginId) {
        try {
            LOCK.readLock().lock();
            return CLASS_LOADERS.computeIfAbsent(pluginId, key -> {
                try {
                    return new PluginClassLoader(new URL[]{URI.create(RequestUtils.getPluginJarUrl(pluginId)).toURL()},
                            Thread.currentThread().getContextClassLoader());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static void initClassLoader(ExternalConfig config) {
        RequestUtils.setApiServer(config.getApiServer());
    }

    public static void useClass(String className, Consumer<Class<?>> consumer) {
        LOCK.readLock().lock();
        try {
            consumer.accept(findOrLoadClass(className));
        } finally {
            LOCK.readLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String className, String hash, Function<Class<T>, T> constructor) {
        try {
            LOCK.readLock().lock();
            return (T) CREATED_OBJECTS.computeIfAbsent(className, key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(hash, key -> {
                        Class<T> clazz = findOrLoadClass(className);
                        return constructor.apply(clazz);
                    });
        } finally {
            LOCK.readLock().unlock();
        }
    }

    private static <T> Function<Class<T>, T> defaultConstruct() {
        return aClass -> {
            try {
                return aClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> void useInstance(String className, String hash, Function<Class<T>, T> constructor, Consumer<T> consumer) {
        LOCK.readLock().lock();
        try {
            consumer.accept(getInstance(className, hash, constructor));
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static <T> void useInstance(String className, String hash, Consumer<T> consumer) {
        useInstance(className, hash, defaultConstruct(), consumer);
    }

    public static <T, R> R callInstance(String className, String hash, Function<Class<T>, T> constructor, Function<T, R> function) {
        LOCK.readLock().lock();
        try {
            return function.apply(getInstance(className, hash, constructor));
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static <T, R> R callInstance(String className, String hash, Function<T, R> function) {
        LOCK.readLock().lock();
        try {
            return function.apply(getInstance(className, hash, defaultConstruct()));
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public synchronized static boolean pluginChanged(String pluginId, Object update, boolean taskManager) {
        Map<String, String> pluginHash = taskManager ? TASK_MANAGER_PLUGIN_HASH : JOB_MANAGER_PLUGIN_HASH;
        if (update instanceof Plugin plugin && plugin.getJarHash().equals(pluginHash.get(pluginId))) {
            return false;
        }
        try {
            LOCK.writeLock().lock();
            PluginClassLoader classLoader = CLASS_LOADERS.remove(pluginId);
            if (classLoader != null) {
                classLoader.close();
            }
            Set<String> expiredClasses = CLASS_NAME_MAP.entrySet().stream().filter(entry -> entry.getValue().equals(pluginId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            for (String expiredClass : expiredClasses) {
                CLASS_NAME_MAP.remove(expiredClass);
                Map<String, Object> objects = CREATED_OBJECTS.remove(expiredClass);
                if (objects != null) {
                    closeObjects(objects.values());
                }
            }
            if (update instanceof Plugin plugin) {
                pluginHash.put(pluginId, plugin.getJarHash());
                plugin.getClasses().forEach(pluginClass -> CLASS_NAME_MAP.put(pluginClass.getClassName(), plugin.getId()));
            } else {
                pluginHash.remove(pluginId);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void clearExpiredClasses(String className, Function<String, Boolean> removeIf) {
        Map<String, Object> objectMap = CREATED_OBJECTS.get(className);
        if (objectMap == null) {
            return;
        }
        Set<String> keys = new HashSet<>(objectMap.keySet());
        keys.forEach(key -> {
            if (removeIf.apply(key)) {
                objectMap.remove(key);
            }
        });
    }

    private static void closeObjects(Collection<Object> objects) {
        objects.forEach(object -> {
            try {
                if (object instanceof Closeable closeable) {
                    closeable.close();
                } else if (object instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized static void close() {
        CLASS_LOADERS.forEach((pluginId, classLoader) -> {
            log.info("unload plugin {}", pluginId);
            try {
                classLoader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        CLASS_LOADERS.clear();
        System.gc();
    }
}
