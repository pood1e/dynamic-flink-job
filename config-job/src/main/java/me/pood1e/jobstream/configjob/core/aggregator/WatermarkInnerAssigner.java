package me.pood1e.jobstream.configjob.core.aggregator;

import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.pluginbase.CfgAggregator;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;

public class WatermarkInnerAssigner implements SerializableTimestampAssigner<DataWrapper<?>> {
    private final CfgConfig cfgConfig;

    public WatermarkInnerAssigner(CfgConfig config) {
        this.cfgConfig = config;
    }

    @Override
    public long extractTimestamp(DataWrapper<?> element, long recordTimestamp) {
        return FunctionUtils.callAggCfg(cfgConfig, String.valueOf(this.hashCode()), (cfg, config) -> {
            CfgAggregator aggregator = (CfgAggregator) cfg;
            return aggregator.getWatermark(element.getData(), recordTimestamp, config);
        });
    }
}
