package me.pood1e.jobstream.plugintest2;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Properties;
import java.util.Set;

@Getter
@Setter
public class KafkaConfig {

    private List<ClusterConfig> clusterConfigs;

    @Getter
    @Setter
    public static class ClusterConfig {
        private String clusterId;
        private Properties properties;
        private Set<String> topics;
    }
}
