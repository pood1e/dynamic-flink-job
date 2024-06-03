package me.pood1e.jobstream.pluginbase;

public interface CfgAggregator<IN, ACC, OUT, C> {
    ACC createAccumulator(C config);

    ACC add(IN in, ACC accumulator, C config);

    OUT getResult(ACC acc, C config);

    ACC merge(ACC a1, ACC a2, C config);

    long getWatermark(IN in, long recordTime, C config);

    default String keyBy(IN in, C config) {
        return null;
    }
}
