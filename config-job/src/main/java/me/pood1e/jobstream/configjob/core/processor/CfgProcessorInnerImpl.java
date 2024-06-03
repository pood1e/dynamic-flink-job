package me.pood1e.jobstream.configjob.core.processor;

import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.side.InnerOutputTag;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import me.pood1e.jobstream.configjob.core.utils.ReflectUtils;
import me.pood1e.jobstream.pluginbase.CfgProcessor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class CfgProcessorInnerImpl extends ProcessFunction<DataWrapper<?>, DataWrapper<?>> {

    private final CfgConfig config;

    public CfgProcessorInnerImpl(CfgConfig config) {
        this.config = config;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        PluginClassLoaderUtils.initClassLoader(config);
        FunctionUtils.assureLoadConfig(config.getId(), true);
    }

    @Override
    public void processElement(DataWrapper<?> value, ProcessFunction<DataWrapper<?>, DataWrapper<?>>.Context ctx, Collector<DataWrapper<?>> out) throws Exception {
        FunctionUtils.useCfg(config.getId(), String.valueOf(this.hashCode()), (cfg, config) -> {
            Class<?> dataClass = ReflectUtils.getInterfaceGenericType(cfg.getClass(), CfgProcessor.class, 0);
            Object data = FunctionUtils.transform(value.getData(), dataClass);
            if (data != null) {
                CfgProcessor<Object, Object, Object> processor = (CfgProcessor<Object, Object, Object>) cfg;
                processor.process(data, config, new CfgProcessor.Collector<>() {
                    @Override
                    public void collect(Object object) {
                        out.collect(DataWrapper.builder().data(object).build());
                    }

                    @Override
                    public void side(String key, Object object) {
                        ctx.output(new InnerOutputTag(key), DataWrapper.builder().data(object).build());
                    }
                });
            }
        });
    }

    @Override
    public void close() throws Exception {
        PluginClassLoaderUtils.close();
    }
}
