package me.pood1e.jobstream.common;

import lombok.Getter;
import me.pood1e.jobstream.pluginbase.CfgAggregator;
import me.pood1e.jobstream.pluginbase.CfgProcessor;
import me.pood1e.jobstream.pluginbase.CfgSink;
import me.pood1e.jobstream.pluginbase.CfgSource;

@Getter
public enum FunctionType {
    SOURCE(CfgSource.class, 1),
    PROCESSOR(CfgProcessor.class, 2),
    SINK(CfgSink.class, 1),
    AGGREGATOR(CfgAggregator.class, 3);
    private final Class<?> baseClass;
    private final int configParamIndex;

    FunctionType(Class<?> baseClass, int configParamIndex) {
        this.baseClass = baseClass;
        this.configParamIndex = configParamIndex;
    }
}
