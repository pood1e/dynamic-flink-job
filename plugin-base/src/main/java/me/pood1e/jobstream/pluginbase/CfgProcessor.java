package me.pood1e.jobstream.pluginbase;

public interface CfgProcessor<IN, OUT, C> {
    interface Collector<OUT> {
        void collect(OUT out);

        void side(String key, Object object);
    }

    void process(IN in, C config, Collector<OUT> collector);
}
