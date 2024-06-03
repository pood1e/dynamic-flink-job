package me.pood1e.jobstream.configjob.core.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.common.FunctionType;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.common.NotificationType;
import me.pood1e.jobstream.configjob.core.config.SourceCfgConfig;
import me.pood1e.jobstream.configjob.core.utils.CompareUtils;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import me.pood1e.jobstream.configjob.core.utils.ReflectUtils;
import me.pood1e.jobstream.pluginbase.CfgSource;
import me.pood1e.jobstream.pluginbase.FetchConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.connector.source.SplitsAssignment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CfgSourceEnumerator implements SplitEnumerator<NotificationSplit, Void>, Notifiable {

    private final SplitEnumeratorContext<NotificationSplit> ctx;
    private final Map<Integer, List<Object>> assignMap = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final SourceCfgConfig config;

    public CfgSourceEnumerator(SourceCfgConfig config, SplitEnumeratorContext<NotificationSplit> ctx) {
        this.ctx = ctx;
        this.config = config;
    }

    @Override
    public void start() {
        PluginClassLoaderUtils.initClassLoader(config);
        FunctionUtils.assureLoadConfig(config.getId(), false);
        FunctionUtils.ENUMERATOR_MAP.put(config.getId(), this);
        if (config.getRebalanceSeconds() == 0) {
            rebalance();
        } else {
            ctx.callAsync(() -> {
                        rebalance();
                        return null;
                    }, (object, throwable) -> log.warn("error cause ", throwable),
                    0, config.getRebalanceSeconds() * 1000L);
        }
    }

    private synchronized void rebalance() {
        List<Integer> subTaskIds = ctx.registeredReaders().keySet().stream().sorted().toList();
        if (subTaskIds.isEmpty()) {
            return;
        }
        Map<Integer, List<Object>> subTaskAssignMap = new HashMap<>();
        Map<Integer, List<NotificationSplit>> splitMap = new HashMap<>();
        FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), cfg -> {
            CfgSource<?, Object, ?> cfgSource = (CfgSource<?, Object, ?>) cfg;
            Class<?> configClass = ReflectUtils.getInterfaceGenericType(cfg.getClass(), CfgSource.class,
                    FunctionType.SOURCE.getConfigParamIndex());
            Object newerConfig = mapper.convertValue(FunctionUtils.getFunctionConfig(config.getId()),
                    configClass);
            List<?> subTaskConfigs = cfgSource.getFetchConfigs(newerConfig);
            for (int i = 0; i < subTaskConfigs.size(); i++) {
                Integer assignId = subTaskIds.get(i % subTaskIds.size());
                subTaskAssignMap.computeIfAbsent(assignId, key -> new ArrayList<>()).add(subTaskConfigs.get(i));
            }
            subTaskAssignMap.forEach((assignId, cs) -> {
                List<Object> old = assignMap.getOrDefault(assignId, new ArrayList<>());
                List<Notification> notifications = CompareUtils.compareAndUpdate(cs, old, new CompareUtils.CompareFunction<>() {
                    @Override
                    public String extractId(Object fetchConfig) {
                        return ((FetchConfig) fetchConfig).getId();
                    }

                    @Override
                    public boolean eq(Object t1, Object t2) {
                        try {
                            return mapper.writeValueAsString(t1).equals(mapper.writeValueAsString(t2));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, false);
                List<NotificationSplit> splits = notifications.stream().map(notification -> {
                    Object fetchConfig = mapper.convertValue(notification.getConfig(), JsonNode.class);
                    notification.setConfig(fetchConfig);
                    return new NotificationSplit(notification);
                }).collect(Collectors.toList());
                if (!splits.isEmpty()) {
                    splitMap.put(assignId, splits);
                }
            });
        }, false);
        if (!subTaskAssignMap.isEmpty()) {
            ctx.assignSplits(new SplitsAssignment<>(splitMap));
            assignMap.clear();
            assignMap.putAll(subTaskAssignMap);
        }
    }

    @Override
    public void handleSplitRequest(int subtaskId, @Nullable String requesterHostname) {
        List<Object> objects = assignMap.get(subtaskId);
        if (CollectionUtils.isEmpty(objects)) {
            return;
        }
        List<Notification> notifications = new ArrayList<>();
        FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), cfg -> {
            Class<?> fetchConfigClass = ReflectUtils.getInterfaceGenericType(cfg.getClass(), CfgSource.class, 2);
            objects.forEach(object -> {
                FetchConfig fetchConfig = (FetchConfig) mapper.convertValue(object, fetchConfigClass);
                notifications.add(Notification.builder()
                        .id(fetchConfig.getId())
                        .type(NotificationType.UPDATE)
                        .className(fetchConfigClass.getName())
                        .config(object)
                        .build());
            });
        }, false);
        notifications.forEach(notification -> ctx.assignSplit(new NotificationSplit(notification), subtaskId));
    }

    @Override
    public void addSplitsBack(List<NotificationSplit> splits, int subtaskId) {
        rebalance();
    }

    @Override
    public void addReader(int subtaskId) {
        rebalance();
    }

    @Override
    public Void snapshotState(long checkpointId) throws Exception {
        // do nothing
        return null;
    }

    @Override
    public void close() throws IOException {
        FunctionUtils.ENUMERATOR_MAP.remove(config.getId());
        PluginClassLoaderUtils.close();
    }

    @Override
    public void notifyChanged() {
        rebalance();
    }
}
