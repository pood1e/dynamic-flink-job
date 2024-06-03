package me.pood1e.jobstream.configjob.core.utils;

import lombok.experimental.UtilityClass;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.common.NotificationType;
import me.pood1e.jobstream.common.Plugin;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class CompareUtils {
    private final static Map<String, Plugin> PLUGIN_MAP = new HashMap<>();

    public static List<Notification> pluginCompare(List<Plugin> plugins) {
        List<Notification> notifications = CompareUtils.compareAndUpdate(plugins, PLUGIN_MAP, new CompareUtils.CompareFunction<>() {
            @Override
            public String extractId(Plugin plugin) {
                return plugin.getId();
            }

            @Override
            public boolean eq(Plugin t1, Plugin t2) {
                return t1.getJarHash().equals(t2.getJarHash());
            }
        }, true);
        notifications.forEach(notification -> PluginClassLoaderUtils.pluginChanged(notification.getId(), notification.getConfig(), false));
        return notifications;
    }


    public interface CompareFunction<T> {
        String extractId(T t);

        boolean eq(T t1, T t2);
    }

    public static <T> List<Notification> compareAndUpdate(List<T> ts, List<T> old, CompareFunction<T> compareFunction, boolean ignoreFirst) {
        return compareAndUpdate(ts, old.stream().collect(Collectors.toMap(compareFunction::extractId, v -> v)), compareFunction, ignoreFirst);
    }

    public static <T> List<Notification> compareAndUpdate(List<T> ts, Map<String, T> tMap, CompareFunction<T> compareFunction, boolean ignoreFirst) {
        Map<String, T> newMap = ts.stream().collect(Collectors.toMap(compareFunction::extractId, t -> t));
        if (tMap.isEmpty()) {
            tMap.putAll(newMap);
            if (ignoreFirst) {
                return List.of();
            } else {
                return ts.stream().map(t -> Notification.builder()
                        .config(t)
                        .id(compareFunction.extractId(t))
                        .className(t.getClass().getName())
                        .type(NotificationType.CREATE)
                        .build()).collect(Collectors.toList());
            }
        }
        List<Notification> notifications = new ArrayList<>();
        // 在old中而不在新集合中, delete
        Set<String> oldKeys = new HashSet<>(tMap.keySet());
        oldKeys.stream().filter(t -> !newMap.containsKey(t))
                .forEach(key -> {
                    T t = tMap.remove(key);
                    notifications.add(Notification.builder()
                            .config(null)
                            .type(NotificationType.DELETE)
                            .id(key)
                            .className(t.getClass().getName())
                            .build());
                });
        // 在new中而不在old中, create
        newMap.keySet().stream().filter(key -> !oldKeys.contains(key))
                .forEach(key -> {
                    T t = newMap.get(key);
                    notifications.add(Notification.builder()
                            .config(t)
                            .id(key)
                            .className(t.getClass().getName())
                            .type(NotificationType.CREATE)
                            .build());
                });
        // 两者都有, 比较, 有差异则进行update
        newMap.keySet().stream()
                .filter(oldKeys::contains)
                .forEach(key -> {
                    T t = newMap.get(key);
                    if (!compareFunction.eq(t, tMap.get(key))) {
                        notifications.add(Notification.builder()
                                .config(t)
                                .id(key)
                                .className(t.getClass().getName())
                                .type(NotificationType.UPDATE)
                                .build());
                    }
                });
        tMap.putAll(newMap);
        return notifications;
    }
}
