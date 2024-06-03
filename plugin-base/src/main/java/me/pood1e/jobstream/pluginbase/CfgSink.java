package me.pood1e.jobstream.pluginbase;

import java.io.Closeable;
import java.io.Serializable;

public interface CfgSink<IN, C> extends Serializable, Closeable {
    void sink(IN input, C config);

    @Override
    default void close() {
    }
}
