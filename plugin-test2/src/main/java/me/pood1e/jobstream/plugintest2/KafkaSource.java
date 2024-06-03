package me.pood1e.jobstream.plugintest2;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.pood1e.jobstream.pluginbase.CfgSource;
import me.pood1e.jobstream.pluginbase.MarkSourceType;
import me.pood1e.jobstream.pluginbase.SourceThreadType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@MarkSourceType(SourceThreadType.MULTI)
public class KafkaSource implements CfgSource<String, KafkaConfig, KafkaFetchConfig> {

    @Builder
    @Getter
    @Setter
    private static class TaskDetail {
        private Set<String> topicPartitions;
        private Future<?> future;
    }

    private final Map<String, AdminClient> adminClients = new HashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, TaskDetail> taskDetailMap = new HashMap<>();
    private final Map<String, Properties> clusterProperties = new HashMap<>();

    @Override
    public List<KafkaFetchConfig> getFetchConfigs(KafkaConfig kafkaConfig) {
        List<KafkaFetchConfig> fetchConfigs = new ArrayList<>();
        kafkaConfig.getClusterConfigs().forEach(clusterConfig -> {
            AdminClient client = adminClients.computeIfAbsent(clusterConfig.getClusterId(), key -> AdminClient.create(clusterConfig.getProperties()));
            DescribeTopicsResult result = client.describeTopics(clusterConfig.getTopics());
            Map<String, KafkaFuture<TopicDescription>> topicNameValues = result.topicNameValues();
            topicNameValues.forEach((topicName, topicDescription) -> {
                try {
                    topicDescription.get().partitions().forEach(topicPartitionInfo -> {
                        KafkaFetchConfig kafkaFetchConfig = KafkaFetchConfig.builder()
                                .id(clusterConfig.getClusterId() + "@" + topicName + "@" + topicPartitionInfo.partition())
                                .clusterId(clusterConfig.getClusterId())
                                .topic(topicName)
                                .partition(topicPartitionInfo.partition())
                                .build();
                        fetchConfigs.add(kafkaFetchConfig);
                    });
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        return fetchConfigs;
    }

    private void createTask(String clusterId, Set<String> partitions, List<TopicPartition> topicPartitionList, ReaderOutput<String> output) {
        Future<?> future = executor.submit(() -> {
            try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(clusterProperties.get(clusterId))) {
                consumer.assign(topicPartitionList);
                while (true) {
                    ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        output.collect(new String(record.value(), StandardCharsets.UTF_8));
                    }
                }
            }
        });
        TaskDetail taskDetail = TaskDetail.builder()
                .topicPartitions(partitions)
                .future(future)
                .build();
        taskDetailMap.put(clusterId, taskDetail);
    }

    @Override
    public synchronized void onConfigChange(Map<String, KafkaFetchConfig> configMap, ReaderOutput<String> output) {
        Map<String, Set<String>> partitions = new HashMap<>();
        configMap.values().forEach(kafkaFetchConfig -> {
            partitions.computeIfAbsent(kafkaFetchConfig.getClusterId(), key -> new HashSet<>())
                    .add(kafkaFetchConfig.getTopic() + "@" + kafkaFetchConfig.getPartition());
            clusterProperties.putIfAbsent(kafkaFetchConfig.getClusterId(), kafkaFetchConfig.getProperties());
        });
        partitions.forEach((clusterId, topicPartitions) -> {
            TaskDetail detail = taskDetailMap.get(clusterId);
            List<TopicPartition> topicPartitionList = topicPartitions.stream().map(name -> {
                int splitIndex = name.lastIndexOf("@");
                String topic = name.substring(0, splitIndex);
                int partition = Integer.parseInt(name.substring(splitIndex + 1));
                return new TopicPartition(topic, partition);
            }).toList();
            if (detail == null) {
                createTask(clusterId, topicPartitions, topicPartitionList, output);
            } else if (CollectionUtils.isEqualCollection(topicPartitions, detail.getTopicPartitions())) {
                detail.getFuture().cancel(true);
                createTask(clusterId, topicPartitions, topicPartitionList, output);
            }
        });
        Set<String> expiredCluster = new HashSet<>(taskDetailMap.keySet());
        expiredCluster.forEach(clusterId -> {
            if (!partitions.containsKey(clusterId)) {
                taskDetailMap.remove(clusterId).getFuture().cancel(true);
            }
        });
    }

    @Override
    public void close() {
        adminClients.values().forEach(Admin::close);
    }
}
