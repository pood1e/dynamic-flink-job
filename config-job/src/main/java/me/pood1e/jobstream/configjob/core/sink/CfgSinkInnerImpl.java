package me.pood1e.jobstream.configjob.core.sink;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import me.pood1e.jobstream.configjob.core.utils.ReflectUtils;
import me.pood1e.jobstream.pluginbase.CfgSink;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

@Slf4j
public class CfgSinkInnerImpl extends RichSinkFunction<DataWrapper<?>> {

    private final CfgConfig config;

    public CfgSinkInnerImpl(CfgConfig config) {
        this.config = config;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        PluginClassLoaderUtils.initClassLoader(config);
        FunctionUtils.assureLoadConfig(config.getId(), true);
    }

    @Override
    public void invoke(DataWrapper<?> value, Context context) {
        FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), (cfg, config) -> {
            Class<?> dataClass = ReflectUtils.getInterfaceGenericType(cfg.getClass(), CfgSink.class, 0);
            Object data = FunctionUtils.transform(value.getData(), dataClass);
            if (data != null) {
                CfgSink<Object, Object> sink = (CfgSink<Object, Object>) cfg;
                sink.sink(data, config);
            }
        });
    }

    @Override
    public void close() throws Exception {
        PluginClassLoaderUtils.close();
    }
}
