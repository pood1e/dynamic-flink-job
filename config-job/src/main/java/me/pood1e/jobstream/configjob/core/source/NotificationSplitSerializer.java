package me.pood1e.jobstream.configjob.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import java.io.IOException;

public class NotificationSplitSerializer implements SimpleVersionedSerializer<NotificationSplit> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public byte[] serialize(NotificationSplit obj) throws IOException {
        return mapper.writeValueAsBytes(obj);
    }

    @Override
    public NotificationSplit deserialize(int version, byte[] serialized) throws IOException {
        return mapper.readValue(serialized, NotificationSplit.class);
    }
}
