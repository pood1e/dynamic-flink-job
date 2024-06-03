package me.pood1e.jobstream.configjob.core.source;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.pluginbase.CfgSource;
import me.pood1e.jobstream.pluginbase.FetchConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class MultiSourceReader extends BaseSourceReadStrategy {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Map<String, Future<?>> futures = new HashMap<>();

    private final Map<String, CompletableFuture<Void>> futureMap = new ConcurrentHashMap<>();

    public MultiSourceReader(CfgConfig config) {
        super(config);
    }

    @Override
    protected void addNotifications(List<Notification> notifications) {
        FunctionUtils.useFetchConfig(config.getId(), String.valueOf(this.hashCode()), (o1, o2) -> notifications.forEach(notification -> {
            switch (notification.getType()) {
                case CREATE -> schedule(notification.getId());
                case UPDATE -> {
                    cancel(notification.getId());
                    schedule(notification.getId());
                }
                case DELETE -> cancel(notification.getId());
            }
        }));
    }

    private void completeFuture(String fetchId) {
        CompletableFuture<Void> future = futureMap.get(fetchId);
        CompletableFuture<Void> newFuture = new CompletableFuture<>();
        futureMap.put(fetchId, newFuture);
        future.complete(null);
    }

    private void schedule(String fetchId) {
        futureMap.put(fetchId, new CompletableFuture<>());
        Future<?> future = executorService.submit(() -> FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), object -> {
            CfgSource source = (CfgSource) object;
            FunctionUtils.useFetchConfig(config.getId(), String.valueOf(this.hashCode()), (o1, o2) -> {
                Map<String, Object> configMap = (Map<String, Object>) o1;
                queue.add(DataWrapper.builder().data(source.fetch((FetchConfig) configMap.get(fetchId))).build());
                completeFuture(fetchId);
            });
        }, true));
        futures.put(fetchId, future);
    }

    private void cancel(String fetchId) {
        Future<?> scheduledFuture = futures.remove(fetchId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        CompletableFuture<Void> future = futureMap.remove(fetchId);
        if (future != null) {
            future.complete(null);
        }
        log.info("Canceled [{}]", fetchId);
    }

    @Override
    public void close() throws Exception {
        executorService.shutdown();
        executorService.close();
        futureMap.values().forEach(future -> future.complete(null));
        log.debug("closed reader {}", this);
    }

    @Override
    public CompletableFuture<Void> isAvailable() {
        Collection<CompletableFuture<Void>> availableFutures = futureMap.values();
        if (futures.isEmpty()) {
            log.debug("no futures available");
            return CompletableFuture.runAsync(() -> log.debug("checking futures available"),
                    CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
        }
        return CompletableFuture.anyOf(availableFutures.toArray(CompletableFuture[]::new))
                .thenAccept(obj -> log.debug("one future completed"));
    }
}
