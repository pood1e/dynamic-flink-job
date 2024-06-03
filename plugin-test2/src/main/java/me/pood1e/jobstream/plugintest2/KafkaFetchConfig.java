package me.pood1e.jobstream.plugintest2;

import lombok.*;
import me.pood1e.jobstream.pluginbase.FetchConfig;

import java.util.Properties;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaFetchConfig implements FetchConfig {
    private String id;
    private String clusterId;
    private String topic;
    private int partition;
    private Properties properties;
}
