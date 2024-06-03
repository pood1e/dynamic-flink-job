package me.pood1e.jobstream.configjob.core.side;

import me.pood1e.jobstream.configjob.core.inner.DataWrapper;
import me.pood1e.jobstream.configjob.core.inner.DataWrapperTypeInformation;
import org.apache.flink.util.OutputTag;

import java.io.Serializable;

public class InnerOutputTag extends OutputTag<DataWrapper<?>> implements Serializable {
    public InnerOutputTag(String id) {
        super(id, new DataWrapperTypeInformation());
    }
}
