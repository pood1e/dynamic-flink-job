package me.pood1e.jobstream.plugintest;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.pluginbase.CfgSink;
import me.pood1e.jobstream.pluginbase.Mark;

@Slf4j
@Mark("test2")
public class TestSink2 implements CfgSink<TestData, TestSinkConfig> {
    @Override
    public void sink(TestData input, TestSinkConfig config) {
        log.info("SINK2 {} : {}", config.getSinkLabel(), input.getValue());
    }
}
