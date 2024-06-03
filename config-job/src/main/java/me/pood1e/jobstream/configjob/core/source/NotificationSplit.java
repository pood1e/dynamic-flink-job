package me.pood1e.jobstream.configjob.core.source;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.pood1e.jobstream.common.Notification;
import org.apache.flink.api.connector.source.SourceSplit;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSplit implements SourceSplit {
    private Notification notification;

    @Override
    public String splitId() {
        return notification.getId();
    }
}
