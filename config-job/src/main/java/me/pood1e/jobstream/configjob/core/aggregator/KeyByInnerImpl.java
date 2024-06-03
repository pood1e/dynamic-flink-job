package me.pood1e.jobstream.configjob.core.aggregator;

import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.pluginbase.CfgAggregator;
import org.apache.flink.api.java.functions.KeySelector;

public class KeyByInnerImpl implements KeySelector<DataWrapper<?>, String> {

    private final CfgConfig cfgConfig;

    public KeyByInnerImpl(CfgConfig config) {
        this.cfgConfig = config;
    }

    @Override
    public String getKey(DataWrapper<?> value) throws Exception {
        return FunctionUtils.callAggCfg(cfgConfig, String.valueOf(this.hashCode()), (cfg, config) -> {
            CfgAggregator aggregator = (CfgAggregator) cfg;
            return aggregator.keyBy(value.getData(), config);
        });
    }
}
