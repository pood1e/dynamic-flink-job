package me.pood1e.jobstream.configjob.core.source;


import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.pluginbase.CfgSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QueueSourceReader extends BaseSourceReadStrategy {

    private CompletableFuture<Void> future = new CompletableFuture<>();

    private final CfgSource.ReaderOutput<Object> output = new CfgSource.ReaderOutput<Object>() {
        @Override
        public void collect(Object o) {
            queue.add(DataWrapper.builder().data(o).build());
            future.complete(null);
            future = new CompletableFuture<>();
        }
    };

    protected QueueSourceReader(CfgConfig config) {
        super(config);
    }

    @Override
    protected void addNotifications(List<Notification> notifications) {
        FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), object -> {
            CfgSource source = (CfgSource) object;
            FunctionUtils.useFetchConfig(config.getId(), String.valueOf(this.hashCode()), (o1, o2) -> {
                Map<String, Object> configMap = (Map<String, Object>) o1;
                source.onConfigChange(configMap, output);
            });
        }, true);
    }

    @Override
    public CompletableFuture<Void> isAvailable() {
        return future;
    }

    @Override
    public void close() throws Exception {
        FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), object -> {
            CfgSource source = (CfgSource) object;
            source.close();
        }, true);
    }
}
