package me.pood1e.jobstream.plugintest;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.pluginbase.CfgSink;

@Slf4j
public class TestAggSink implements CfgSink<TestAggData, TestAggConfig> {
    @Override
    public void sink(TestAggData input, TestAggConfig config) {
        log.info("{}: {}", config.getValue(), input.getCount());
    }
}
