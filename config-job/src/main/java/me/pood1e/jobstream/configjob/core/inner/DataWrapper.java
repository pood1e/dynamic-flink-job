package me.pood1e.jobstream.configjob.core.inner;

import lombok.*;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeInfo(DataWrapperTypeFactory.class)
public class DataWrapper<T> implements Serializable {
    private T data;
}
