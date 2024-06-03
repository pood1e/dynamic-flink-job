package me.pood1e.jobstream.configjob.core.inner;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.lang.reflect.Type;
import java.util.Map;

public class DataWrapperTypeFactory extends TypeInfoFactory<DataWrapper<?>> {

    @Override
    public TypeInformation<DataWrapper<?>> createTypeInfo(Type t, Map<String, TypeInformation<?>> genericParameters) {
        return new DataWrapperTypeInformation();
    }
}
