package me.pood1e.jobstream.configjob.core.source;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.utils.FunctionUtils;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import me.pood1e.jobstream.pluginbase.MarkSourceType;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.core.io.InputStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
public class SourceReaderWrapper implements SourceReader<DataWrapper<?>, NotificationSplit>, Notifiable {

    private final CfgConfig config;
    private final Function<Class<SourceReader<DataWrapper<?>, NotificationSplit>>, SourceReader<DataWrapper<?>, NotificationSplit>> constructor;
    private final SourceReaderContext context;

    public SourceReaderWrapper(CfgConfig config, SourceReaderContext context) {
        this.config = config;
        this.constructor = clazz -> {
            MarkSourceType sourceType = clazz.getDeclaredAnnotation(MarkSourceType.class);
            switch (sourceType.value()) {
                case MULTI -> {
                    return new MultiSourceReader(config);
                }
                case MULTI_PERIOD -> {
                    return new MultiPeriodSourceReader(config);
                }
                case SINGLE -> {
                    return new SingleBlockingSourceReader(config);
                }
                case QUEUE -> {
                    return new QueueSourceReader(config);
                }
            }
            throw new RuntimeException();
        };
        this.context = context;
    }

    @Override
    public void start() {
        PluginClassLoaderUtils.initClassLoader(config);
        FunctionUtils.assureLoadConfig(config.getId(), true);
        FunctionUtils.READER_MAP.computeIfAbsent(config.getId(), key -> new ArrayList<>()).add(this);
    }

    @Override
    public InputStatus pollNext(ReaderOutput<DataWrapper<?>> output) {
        return FunctionUtils.callSourceReader(config.getId(), String.valueOf(this.hashCode()), constructor, reader -> {
            try {
                return reader.pollNext(output);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<NotificationSplit> snapshotState(long checkpointId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> isAvailable() {
        return FunctionUtils.callSourceReader(config.getId(), String.valueOf(this.hashCode()), constructor, SourceReader::isAvailable);
    }

    @Override
    public void addSplits(List<NotificationSplit> splits) {
        FunctionUtils.useSourceReader(config.getId(), String.valueOf(this.hashCode()), constructor, reader -> reader.addSplits(splits));
    }

    @Override
    public void notifyNoMoreSplits() {

    }

    @Override
    public void close() throws Exception {
        FunctionUtils.READER_MAP.get(config.getId()).remove(this);
        PluginClassLoaderUtils.close();
    }

    @Override
    public void notifyChanged() {
        context.sendSplitRequest();
    }
}
