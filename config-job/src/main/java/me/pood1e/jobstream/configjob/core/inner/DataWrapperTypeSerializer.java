package me.pood1e.jobstream.configjob.core.inner;

import com.esotericsoftware.kryo.Kryo;
import me.pood1e.jobstream.configjob.core.utils.PluginClassLoaderUtils;
import org.apache.flink.api.common.serialization.SerializerConfigImpl;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;
import java.util.function.Function;

public class DataWrapperTypeSerializer extends TypeSerializer<DataWrapper<?>> {

    private static final String TYPE_SERIALIZER_HASH = "type-serializer";
    private static final Function<Class<TypeSerializer>, TypeSerializer> FUNCTION = objectClass -> {
        TypeInformation<?> typeInfo = TypeExtractor.createTypeInfo(objectClass);
        return typeInfo.createSerializer(new SerializerConfigImpl());
    };

    @Override
    public boolean isImmutableType() {
        return true;
    }

    @Override
    public TypeSerializer<DataWrapper<?>> duplicate() {
        return this;
    }

    @Override
    public DataWrapper<?> createInstance() {
        return DataWrapper.builder().build();
    }

    @Override
    public DataWrapper<?> copy(DataWrapper<?> from) {
        return DataWrapper.builder().data(new Kryo().copy(from.getData())).build();
    }

    @Override
    public DataWrapper<?> copy(DataWrapper<?> from, DataWrapper<?> reuse) {
        return reuse;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public void serialize(DataWrapper<?> record, DataOutputView target) throws IOException {
        Class<?> clazz = record.getData().getClass();
        target.writeUTF(clazz.getName());
        PluginClassLoaderUtils.useInstance(clazz.getName(), TYPE_SERIALIZER_HASH, FUNCTION, serializer -> {
            try {
                serializer.serialize(record.getData(), target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public DataWrapper<?> deserialize(DataInputView source) throws IOException {
        String className = source.readUTF();
        Object object = PluginClassLoaderUtils.callInstance(className, TYPE_SERIALIZER_HASH, FUNCTION, serializer -> {
            try {
                return serializer.deserialize(source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return DataWrapper.builder().data(object).build();
    }

    @Override
    public DataWrapper<?> deserialize(DataWrapper<?> reuse, DataInputView source) throws IOException {
        if (reuse != null) {
            return reuse;
        }
        return deserialize(source);
    }

    @Override
    public void copy(DataInputView source, DataOutputView target) throws IOException {

    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public TypeSerializerSnapshot<DataWrapper<?>> snapshotConfiguration() {
        return null;
    }
}
