package me.pood1e.jobstream.configjob.core.inner;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.Notification;
import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.configjob.core.source.Notifiable;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.util.ArrayList;
import java.util.List;

public class InnerSink extends RichSinkFunction<Notification> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void invoke(Notification value, Context context) throws Exception {
        if (value.getClassName().equals(Function.class.getName())) {
            FunctionUtils.updateConfig(value.getId(), objectMapper.convertValue(value.getConfig(), Function.class));
        }
        if (value.getClassName().equals(Plugin.class.getName())) {
            List<String> functionIds = FunctionUtils.pluginEffectFunctionIds(value.getId());
            boolean updated = PluginClassLoaderUtils.pluginChanged(value.getId(), objectMapper.convertValue(value.getConfig(), Plugin.class), true);
            if (updated) {
                functionIds.forEach(functionId -> {
                    FunctionUtils.READER_MAP.getOrDefault(functionId, new ArrayList<>()).forEach(Notifiable::notifyChanged);
                });
            }
        }
    }

    @Override
    public void close() throws Exception {
        PluginClassLoaderUtils.close();
    }
}
