package me.pood1e.jobstream.configjob.core.source;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.core.io.InputStatus;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@Slf4j
public abstract class BaseSourceReadStrategy implements SourceReader<DataWrapper<?>, NotificationSplit> {
    protected final CfgConfig config;
    protected final Queue<DataWrapper<?>> queue = new ArrayDeque<>();

    protected BaseSourceReadStrategy(CfgConfig config) {
        this.config = config;
    }

    @Override
    public void addSplits(List<NotificationSplit> splits) {
        List<Notification> notifications = splits.stream()
                .map(NotificationSplit::getNotification)
                .toList();
        FunctionUtils.modifyFetchConfig(config.getId(), String.valueOf(this.hashCode()), notifications);
        addNotifications(notifications);
    }

    @Override
    public void notifyNoMoreSplits() {

    }

    @Override
    public void start() {

    }

    @Override
    public InputStatus pollNext(ReaderOutput<DataWrapper<?>> output) throws Exception {
        DataWrapper<?> dataWrapper = queue.poll();
        if (dataWrapper != null) {
            log.debug("write one data to queue");
            output.collect(dataWrapper);
            return InputStatus.MORE_AVAILABLE;
        }
        return InputStatus.NOTHING_AVAILABLE;
    }

    @Override
    public List<NotificationSplit> snapshotState(long checkpointId) {
        return List.of();
    }

    protected abstract void addNotifications(List<Notification> notifications);
}
