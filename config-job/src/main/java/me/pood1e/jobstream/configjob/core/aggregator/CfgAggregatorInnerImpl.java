package me.pood1e.jobstream.configjob.core.aggregator;

import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import me.pood1e.jobstream.pluginbase.CfgAggregator;
import org.apache.flink.api.common.functions.AggregateFunction;

public class CfgAggregatorInnerImpl implements AggregateFunction<DataWrapper<?>, DataWrapper<?>, DataWrapper<?>> {

    private final CfgConfig cfgConfig;

    public CfgAggregatorInnerImpl(CfgConfig config) {
        this.cfgConfig = config;
    }

    @Override
    public DataWrapper<?> createAccumulator() {
        PluginClassLoaderUtils.initClassLoader(cfgConfig);
        FunctionUtils.assureLoadConfig(cfgConfig.getId(), true);
        Object data = FunctionUtils.callAggCfg(cfgConfig, String.valueOf(this.hashCode()), (cfg, config) -> {
            CfgAggregator aggregator = (CfgAggregator) cfg;
            return aggregator.createAccumulator(config);
        });
        return DataWrapper.builder().data(data).build();
    }

    @Override
    public DataWrapper<?> add(DataWrapper<?> value, DataWrapper<?> accumulator) {
        PluginClassLoaderUtils.initClassLoader(cfgConfig);
        FunctionUtils.assureLoadConfig(cfgConfig.getId(), true);
        Object data = FunctionUtils.callAggCfg(cfgConfig, String.valueOf(this.hashCode()), (cfg, config) -> {
            CfgAggregator aggregator = (CfgAggregator) cfg;
            return aggregator.add(value.getData(), accumulator.getData(), config);
        });
        return DataWrapper.builder().data(data).build();
    }

    @Override
    public DataWrapper<?> getResult(DataWrapper<?> accumulator) {
        PluginClassLoaderUtils.initClassLoader(cfgConfig);
        FunctionUtils.assureLoadConfig(cfgConfig.getId(), true);
        Object data = FunctionUtils.callAggCfg(cfgConfig, String.valueOf(this.hashCode()), (cfg, config) -> {
            CfgAggregator aggregator = (CfgAggregator) cfg;
            return aggregator.getResult(accumulator.getData(), config);
        });
        return DataWrapper.builder().data(data).build();
    }

    @Override
    public DataWrapper<?> merge(DataWrapper<?> a, DataWrapper<?> b) {
        PluginClassLoaderUtils.initClassLoader(cfgConfig);
        FunctionUtils.assureLoadConfig(cfgConfig.getId(), true);
        Object data = FunctionUtils.callAggCfg(cfgConfig, String.valueOf(this.hashCode()), (cfg, config) -> {
            CfgAggregator aggregator = (CfgAggregator) cfg;
            return aggregator.merge(a.getData(), b.getData(), config);
        });
        return DataWrapper.builder().data(data).build();
    }
}
