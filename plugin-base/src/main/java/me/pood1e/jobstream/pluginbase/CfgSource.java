package me.pood1e.jobstream.pluginbase;

import java.io.Closeable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface CfgSource<T, S, C extends FetchConfig> extends Serializable, Closeable {
    default T fetch(Map<String, C> configMap) {
        return null;
    }

    default T fetch(C config) {
        return null;
    }

    default void onConfigChange(Map<String, C> configMap, ReaderOutput<T> output) {}

    List<C> getFetchConfigs(S s);

    @Override
    default void close() {
    }

    interface ReaderOutput<T> {
        void collect(T t);
    }
}
