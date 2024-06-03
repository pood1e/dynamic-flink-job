package me.pood1e.jobstream.configjob.core.source;

import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.pluginbase.CfgSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SingleBlockingSourceReader extends BaseSourceReadStrategy {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private CompletableFuture<Void> future = new CompletableFuture<>();

    public SingleBlockingSourceReader(CfgConfig config) {
        super(config);
    }

    @Override
    protected void addNotifications(List<Notification> notifications) {
        executorService.scheduleWithFixedDelay(() -> {
            FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), object -> {
                CfgSource source = (CfgSource) object;
                FunctionUtils.useFetchConfig(config.getId(), String.valueOf(this.hashCode()), (o1, o2) -> {
                    Map<String, Object> configMap = (Map<String, Object>) o1;
                    queue.add(DataWrapper.builder().data(source.fetch(configMap)).build());
                    CompletableFuture<Void> temp = future;
                    future = new CompletableFuture<>();
                    temp.complete(null);
                });
            }, true);
        }, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        executorService.shutdown();
        executorService.close();
        future.complete(null);
    }

    @Override
    public CompletableFuture<Void> isAvailable() {
        return future;
    }
}
