package me.pood1e.jobstream.configjob.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.FunctionType;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.common.NotificationType;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.source.Notifiable;
import me.pood1e.jobstream.pluginbase.FetchConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@UtilityClass
public class FunctionUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static boolean firstUpdate = true;

    private static final Map<String, Function<?>> CONFIG_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Function<?>> TASK_MANAGER_CONFIG_MAP = new ConcurrentHashMap<>();

    public static final Map<String, Notifiable> ENUMERATOR_MAP = new ConcurrentHashMap<>();

    public static final Map<String, List<Notifiable>> READER_MAP = new ConcurrentHashMap<>();

    public static void assureLoadConfig(String id, boolean taskManager) {
        Map<String, Function<?>> configMap = taskManager ? TASK_MANAGER_CONFIG_MAP : CONFIG_MAP;
        if (!configMap.containsKey(id)) {
            configMap.put(id, RequestUtils.getFunctionConfig(id));
        }
    }

    public static List<Notification> compareOrUpdate(List<Function<?>> configs) {
        List<Notification> result = CompareUtils.compareAndUpdate(configs, CONFIG_MAP, new CompareUtils.CompareFunction<Function<?>>() {
            @Override
            public String extractId(Function<?> function) {
                return function.getId();
            }

            @Override
            public boolean eq(Function<?> t1, Function<?> t2) {
                try {
                    String oldStr = MAPPER.writeValueAsString(t1);
                    String newStr = MAPPER.writeValueAsString(t2);
                    return oldStr.equals(newStr);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, true);
        if (firstUpdate) {
            firstUpdate = false;
            return List.of();
        }
        if (result.stream().anyMatch(notification -> notification.getType() != NotificationType.UPDATE)) {
            throw new RuntimeException();
        }
        return result;
    }

    public synchronized static void updateConfig(String id, Function<?> config) {
        Function<?> oldConfig = TASK_MANAGER_CONFIG_MAP.get(id);
        if (oldConfig != null) {
            TASK_MANAGER_CONFIG_MAP.put(id, config);
            if (config.getType().equals(FunctionType.SOURCE) && !config.getImpl().equals(oldConfig.getImpl())) {
                READER_MAP.get(id).forEach(Notifiable::notifyChanged);
            }
            useCfgConfigClass(config.getId(), clazz -> PluginClassLoaderUtils.clearExpiredClasses(clazz.getName(), name -> name.startsWith(config.getId())));
        }
    }

    public interface ConsumerTwo {
        void accept(Object o1, Object o2);
    }

    public interface CallbackTwo<T> {
        T call(Object o1, Object o2);
    }

    public static void useCfg(String functionId, String key, ConsumerTwo consumer) {
        Function<?> function = TASK_MANAGER_CONFIG_MAP.get(functionId);
        PluginClassLoaderUtils.useInstance(function.getImpl(), key, cfg -> {
            Class<?> configClass = ReflectUtils.getInterfaceGenericType(cfg.getClass(), function.getType().getBaseClass(),
                    function.getType().getConfigParamIndex());
            Object config = PluginClassLoaderUtils.getInstance(configClass.getName(), functionId, clazz -> MAPPER.convertValue(function.getConfig(), clazz));
            consumer.accept(cfg, config);
        });
    }

    public static void useCfg(String functionId, String key, Consumer<Object> consumer, boolean taskManager) {
        Map<String, Function<?>> configMap = taskManager ? TASK_MANAGER_CONFIG_MAP : CONFIG_MAP;
        Function<?> function = configMap.get(functionId);
        PluginClassLoaderUtils.useInstance(function.getImpl(), key, consumer);
    }

    public static <T> T callAggCfg(CfgConfig cfgConfig, String key, CallbackTwo<T> callbackTwo) {
        PluginClassLoaderUtils.initClassLoader(cfgConfig);
        FunctionUtils.assureLoadConfig(cfgConfig.getId(), true);
        Function<?> function = TASK_MANAGER_CONFIG_MAP.get(cfgConfig.getId());
        return PluginClassLoaderUtils.callInstance(function.getImpl(), key, cfg -> {
            Class<?> configClass = ReflectUtils.getInterfaceGenericType(cfg.getClass(), function.getType().getBaseClass(),
                    function.getType().getConfigParamIndex());
            Object config = PluginClassLoaderUtils.getInstance(configClass.getName(), cfgConfig.getId(), clazz -> MAPPER.convertValue(function.getConfig(), clazz));
            return callbackTwo.call(cfg, config);
        });
    }

    public static void modifyFetchConfig(String functionId, String hash, List<Notification> notifications) {
        useFetchConfig(functionId, hash, (o1, o2) -> {
            Map<String, Object> configMap = (Map<String, Object>) o1;
            notifications.forEach(notification -> {
                FetchConfig config = (FetchConfig) MAPPER.convertValue(notification.getConfig(), (Class<?>) o2);
                switch (notification.getType()) {
                    case CREATE, UPDATE -> configMap.put(notification.getId(), config);
                    case DELETE -> configMap.remove(notification.getId());
                }
            });
        });
    }

    public static void useFetchConfig(String functionId, String hash, ConsumerTwo consumerTwo) {
        Function<?> function = TASK_MANAGER_CONFIG_MAP.get(functionId);
        PluginClassLoaderUtils.useClass(function.getImpl(), aClass -> {
            Class<?> configClass = ReflectUtils.getInterfaceGenericType(aClass, function.getType().getBaseClass(), 2);
            PluginClassLoaderUtils.useInstance(configClass.getName(), functionId + "@" + hash,
                    clazz -> new HashMap<>(),
                    object -> consumerTwo.accept(object, configClass));
        });
    }

    public static void useCfgConfigClass(String functionId, Consumer<Class<?>> consumer) {
        Function<?> function = TASK_MANAGER_CONFIG_MAP.get(functionId);
        PluginClassLoaderUtils.useClass(function.getImpl(), aClass -> {
            Class<?> configClass = ReflectUtils.getInterfaceGenericType(aClass, function.getType().getBaseClass(), function.getType().getConfigParamIndex());
            PluginClassLoaderUtils.useClass(configClass.getName(), consumer);
        });
    }

    public static <T, R> R callSourceReader(String functionId, String hash,
                                            java.util.function.Function<Class<T>, T> constructor,
                                            java.util.function.Function<T, R> mapper) {
        Function<?> function = TASK_MANAGER_CONFIG_MAP.get(functionId);
        return PluginClassLoaderUtils.callInstance(function.getImpl(), hash, constructor, mapper);
    }

    public static <T> void useSourceReader(String functionId, String hash, java.util.function.Function<Class<T>, T> constructor, Consumer<T> consumer) {
        Function<?> function = TASK_MANAGER_CONFIG_MAP.get(functionId);
        PluginClassLoaderUtils.useInstance(function.getImpl(), hash, constructor, consumer);
    }

    public static Object getFunctionConfig(String functionId) {
        return CONFIG_MAP.get(functionId).getConfig();
    }

    public static List<String> pluginEffectFunctionIds(String pluginId) {
        List<String> effectClasses = PluginClassLoaderUtils.getPluginClass(pluginId);
        return TASK_MANAGER_CONFIG_MAP.values().stream().filter(function -> effectClasses.contains(function.getImpl()))
                .map(Function::getId).toList();
    }

    public static Object transform(Object source, Class<?> targetClass) {
        if (source == null) {
            return null;
        }
        if (source.getClass().equals(targetClass)) {
            return source;
        }
        try {
            return MAPPER.convertValue(source, targetClass);
        } catch (Exception e) {
            log.error("Failed to convert data to {}", targetClass, e);
        }
        return null;
    }
}
