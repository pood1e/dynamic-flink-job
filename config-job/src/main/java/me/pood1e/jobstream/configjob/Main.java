package me.pood1e.jobstream.configjob;

import me.pood1e.jobstream.common.ClusterInfo;
import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.Job;
import me.pood1e.jobstream.common.Pipe;
import me.pood1e.jobstream.common.utils.GraphUtils;
import me.pood1e.jobstream.configjob.core.aggregator.CfgAggregatorInnerImpl;
import me.pood1e.jobstream.configjob.core.aggregator.KeyByInnerImpl;
import me.pood1e.jobstream.configjob.core.aggregator.WatermarkInnerAssigner;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.config.InnerSourceConfig;
import me.pood1e.jobstream.configjob.core.config.SourceCfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.inner.InnerSink;
import me.pood1e.jobstream.configjob.core.inner.InnerSource;
import me.pood1e.jobstream.configjob.core.processor.CfgProcessorInnerImpl;
import me.pood1e.jobstream.configjob.core.side.InnerOutputTag;
import me.pood1e.jobstream.configjob.core.sink.CfgSinkInnerImpl;
import me.pood1e.jobstream.configjob.core.source.CfgSourceInnerImpl;
import me.pood1e.jobstream.configjob.core.utils.RequestUtils;
import org.apache.commons.cli.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option apiServer = Option.builder()
                .required(true)
                .longOpt("api.server")
                .hasArg()
                .desc("api server address")
                .build();
        Option jobId = Option.builder()
                .required(true)
                .longOpt("job.id")
                .hasArg()
                .desc("job config id")
                .build();
        options.addOption(apiServer);
        options.addOption(jobId);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        String apiServerAddress = cmd.getOptionValue(apiServer);
        RequestUtils.setApiServer(apiServerAddress);
        String job = cmd.getOptionValue(jobId);
        createJob(apiServerAddress, job);
    }

    private static void createJob(String apiServer, String jobId) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        List<Function<?>> functions = RequestUtils.getFunctionConfigs(jobId);
        ClusterInfo info = RequestUtils.getClusterInfo();
        int innerSinkParallelism = Math.min(info.getWorkers(), functions.stream().map(Function::getParallelism).max(Integer::compareTo).orElse(1));
        Job job = RequestUtils.getJob(jobId);
        InnerSourceConfig innerSourceConfig = InnerSourceConfig.builder()
                .jobId(jobId)
                .apiServer(apiServer)
                .syncPeriod(job.getSyncPeriod())
                .build();
        env.fromSource(new InnerSource(innerSourceConfig), WatermarkStrategy.noWatermarks(), "inner-source")
                .setParallelism(1)
                .broadcast()
                .addSink(new InnerSink())
                .setParallelism(innerSinkParallelism)
                .name("inner-sink");
        Map<String, Function<?>> functionMap = functions.stream().collect(Collectors.toMap(Function::getId, v -> v));

        Map<String, GraphUtils.FunctionRelation> relationMap = GraphUtils.analyzeFunctionRelations(job.getPipes());
        List<List<String>> levels = GraphUtils.analyzeLevels(relationMap);
        Map<String, SingleOutputStreamOperator<DataWrapper<?>>> outMap = new HashMap<>();
        levels.forEach(functionIds -> functionIds.forEach(functionId -> {
            Function<?> function = functionMap.get(functionId);
            CfgConfig config = CfgConfig.builder()
                    .id(functionId)
                    .apiServer(apiServer)
                    .className(function.getImpl())
                    .build();
            switch (function.getType()) {
                case SOURCE -> {
                    SourceCfgConfig sourceCfgConfig = SourceCfgConfig.builder()
                            .id(functionId)
                            .apiServer(apiServer)
                            .className(function.getImpl())
                            .rebalanceSeconds(function.getRebalanceSeconds())
                            .build();
                    SingleOutputStreamOperator<DataWrapper<?>> source = env.fromSource(new CfgSourceInnerImpl(sourceCfgConfig),
                            WatermarkStrategy.noWatermarks(), function.getName()).setParallelism(function.getParallelism());
                    outMap.put(functionId, source);
                }
                case SINK -> {
                    DataStream<DataWrapper<?>> prev = getPrevDataStream(outMap, relationMap.get(functionId));
                    if (prev != null) {
                        prev.addSink(new CfgSinkInnerImpl(config)).setParallelism(function.getParallelism())
                                .name(function.getName());
                    }
                }
                case PROCESSOR -> {
                    DataStream<DataWrapper<?>> prev = getPrevDataStream(outMap, relationMap.get(functionId));
                    if (prev != null) {
                        SingleOutputStreamOperator<DataWrapper<?>> operator = prev.process(new CfgProcessorInnerImpl(config))
                                .setParallelism(function.getParallelism())
                                .name(function.getName());
                        outMap.put(functionId, operator);
                    }
                }
                case AGGREGATOR -> {
                    DataStream<DataWrapper<?>> prev = getPrevDataStream(outMap, relationMap.get(functionId));
                    if (prev != null) {
                        SingleOutputStreamOperator<DataWrapper<?>> operator = prev.assignTimestampsAndWatermarks(
                                WatermarkStrategy.<DataWrapper<?>>forBoundedOutOfOrderness(Duration.ofSeconds(function.getBoundedOutOfOrder()))
                                        .withTimestampAssigner(new WatermarkInnerAssigner(config)));
                        AggregateFunction<DataWrapper<?>, DataWrapper<?>, DataWrapper<?>> aggregateFunction = new CfgAggregatorInnerImpl(config);
                        TumblingEventTimeWindows windowAssigner = TumblingEventTimeWindows.of(Duration.ofSeconds(function.getWindowSeconds()));
                        if (function.isKeyBy()) {
                            operator = operator.keyBy(new KeyByInnerImpl(config))
                                    .window(windowAssigner)
                                    .aggregate(aggregateFunction);
                        } else {
                            operator = operator.windowAll(windowAssigner)
                                    .aggregate(aggregateFunction);
                        }
                        operator = operator.setParallelism(function.getParallelism())
                                .name(function.getName());
                        outMap.put(functionId, operator);
                    }
                }
            }
        }));

        env.configure(new Configuration(), Thread.currentThread().getContextClassLoader());
        env.execute(job.getName());
    }

    private static DataStream<DataWrapper<?>> getPrevDataStream(Map<String, SingleOutputStreamOperator<DataWrapper<?>>> outMap, GraphUtils.FunctionRelation relation) {
        Iterator<Pipe> iterator = relation.getPrev().iterator();
        DataStream<DataWrapper<?>> prev = null;
        while (iterator.hasNext()) {
            Pipe pipe = iterator.next();
            SingleOutputStreamOperator<DataWrapper<?>> operator = outMap.get(pipe.getSource());
            DataStream<DataWrapper<?>> current = pipe.getKey() != null ? operator.getSideOutput(new InnerOutputTag(pipe.getKey())) : operator;
            if (pipe.isBroadcast()) {
                current = current.broadcast();
            }
            if (prev == null) {
                prev = current;
            } else {
                prev = prev.union(current);
            }
        }
        return prev;
    }
}
