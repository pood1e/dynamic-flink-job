package me.pood1e.jobstream.configjob.core.inner;

import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.FunctionType;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.configjob.core.config.InnerSourceConfig;
import me.pood1e.jobstream.configjob.core.source.Notifiable;
import me.pood1e.jobstream.configjob.core.source.NotificationSplit;
import me.pood1e.jobstream.configjob.core.source.NotificationSplitSerializer;
import me.pood1e.jobstream.configjob.core.utils.CompareUtils;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import me.pood1e.jobstream.configjob.core.utils.RequestUtils;
import org.apache.flink.api.connector.source.*;
import org.apache.flink.core.io.InputStatus;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InnerSource implements Source<Notification, NotificationSplit, Void> {

    private final InnerSourceConfig config;

    public InnerSource(InnerSourceConfig config) {
        this.config = config;
    }


    @Override
    public Boundedness getBoundedness() {
        return Boundedness.CONTINUOUS_UNBOUNDED;
    }

    @Override
    public SplitEnumerator<NotificationSplit, Void> createEnumerator(SplitEnumeratorContext<NotificationSplit> enumContext) throws Exception {
        return new Enumerator(config, enumContext);
    }

    @Override
    public SplitEnumerator<NotificationSplit, Void> restoreEnumerator(SplitEnumeratorContext<NotificationSplit> enumContext, Void checkpoint) throws Exception {
        return new Enumerator(config, enumContext);
    }

    @Override
    public SimpleVersionedSerializer<NotificationSplit> getSplitSerializer() {
        return new NotificationSplitSerializer();
    }

    @Override
    public SimpleVersionedSerializer<Void> getEnumeratorCheckpointSerializer() {
        return null;
    }

    @Override
    public SourceReader<Notification, NotificationSplit> createReader(SourceReaderContext readerContext) throws Exception {
        return new Reader();
    }

    public static class Enumerator implements SplitEnumerator<NotificationSplit, Void> {

        private final InnerSourceConfig innerConfig;

        private final SplitEnumeratorContext<NotificationSplit> context;

        public Enumerator(InnerSourceConfig innerConfig, SplitEnumeratorContext<NotificationSplit> context) {
            this.innerConfig = innerConfig;
            this.context = context;
        }

        @Override
        public void start() {
            PluginClassLoaderUtils.initClassLoader(innerConfig);
            if (innerConfig.getSyncPeriod() <= 0) {
                return;
            }
            context.callAsync(() -> {
                List<Plugin> plugins = RequestUtils.getJobPlugins(innerConfig.getJobId());
                List<Notification> notifications = new ArrayList<>(CompareUtils.pluginCompare(plugins));
                List<Function<?>> functions = RequestUtils.getFunctionConfigs(innerConfig.getJobId());
                List<Notification> functionNotifications = FunctionUtils.compareOrUpdate(functions);
                notifications.addAll(functionNotifications);
                return notifications;
            }, (notifications, throwable) -> {
                if (throwable != null) {
                    throw new RuntimeException(throwable);
                }
                notifications.forEach(functionNotification -> {
                    if (functionNotification.getConfig() instanceof Function<?> function && function.getType().equals(FunctionType.SOURCE)) {
                        FunctionUtils.ENUMERATOR_MAP.get(functionNotification.getId()).notifyChanged();
                    }
                });

                List<NotificationSplit> notificationSplits = notifications.stream()
                        .map(NotificationSplit::new)
                        .collect(Collectors.toList());
                List<Integer> subtaskIds = context.registeredReaders().keySet().stream().toList();
                if (subtaskIds.isEmpty() || notificationSplits.isEmpty()) {
                    return;
                }
                Map<Integer, List<NotificationSplit>> newConfigs = new HashMap<>();
                newConfigs.put(subtaskIds.stream().findFirst().get(), notificationSplits);
                context.assignSplits(new SplitsAssignment<>(newConfigs));
            }, 0, innerConfig.getSyncPeriod());
        }

        @Override
        public void handleSplitRequest(int subtaskId, @Nullable String requesterHostname) {

        }

        @Override
        public void addSplitsBack(List<NotificationSplit> splits, int subtaskId) {

        }

        @Override
        public void addReader(int subtaskId) {

        }

        @Override
        public Void snapshotState(long checkpointId) throws Exception {
            return null;
        }

        @Override
        public void close() throws IOException {
            PluginClassLoaderUtils.close();
        }
    }

    public static class Reader implements SourceReader<Notification, NotificationSplit> {

        private final Queue<NotificationSplit> remainingSplits = new ArrayDeque<>();
        private CompletableFuture<Void> availability = new CompletableFuture<>();

        @Override
        public void start() {

        }

        @Override
        public InputStatus pollNext(ReaderOutput<Notification> output) throws Exception {
            NotificationSplit notificationSplit = remainingSplits.poll();
            if (notificationSplit != null) {
                output.collect(notificationSplit.getNotification());
                return InputStatus.MORE_AVAILABLE;
            }
            return InputStatus.NOTHING_AVAILABLE;
        }

        @Override
        public List<NotificationSplit> snapshotState(long checkpointId) {
            return List.of();
        }

        @Override
        public CompletableFuture<Void> isAvailable() {
            return availability;
        }

        @Override
        public void addSplits(List<NotificationSplit> splits) {
            remainingSplits.addAll(splits);
            availability.complete(null);
            availability = new CompletableFuture<>();
        }

        @Override
        public void notifyNoMoreSplits() {

        }

        @Override
        public void close() throws Exception {

        }
    }

}
