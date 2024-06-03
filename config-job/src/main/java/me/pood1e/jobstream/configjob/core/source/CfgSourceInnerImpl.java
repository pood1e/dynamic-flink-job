package me.pood1e.jobstream.configjob.core.source;

import me.pood1e.jobstream.configjob.core.config.CfgConfig;
import me.pood1e.jobstream.configjob.core.config.SourceCfgConfig;
import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import org.apache.flink.api.connector.source.*;
import org.apache.flink.core.io.SimpleVersionedSerializer;

public class CfgSourceInnerImpl implements Source<DataWrapper<?>, NotificationSplit, Void> {

    private final SourceCfgConfig cfgConfig;

    public CfgSourceInnerImpl(SourceCfgConfig cfgConfig) {
        this.cfgConfig = cfgConfig;
    }

    @Override
    public Boundedness getBoundedness() {
        return Boundedness.CONTINUOUS_UNBOUNDED;
    }

    @Override
    public SplitEnumerator<NotificationSplit, Void> createEnumerator(SplitEnumeratorContext<NotificationSplit> enumContext) throws Exception {
        return new CfgSourceEnumerator(cfgConfig, enumContext);
    }

    @Override
    public SplitEnumerator<NotificationSplit, Void> restoreEnumerator(SplitEnumeratorContext<NotificationSplit> enumContext, Void checkpoint) throws Exception {
        return new CfgSourceEnumerator(cfgConfig, enumContext);
    }

    @Override
    public SimpleVersionedSerializer<NotificationSplit> getSplitSerializer() {
        return new NotificationSplitSerializer();
    }

    @Override
    public SimpleVersionedSerializer<Void> getEnumeratorCheckpointSerializer() {
        return null;
    }

    @Override
    public SourceReader<DataWrapper<?>, NotificationSplit> createReader(SourceReaderContext readerContext) throws Exception {
        return new SourceReaderWrapper(cfgConfig, readerContext);
    }
}
