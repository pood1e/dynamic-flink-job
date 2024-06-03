package me.pood1e.jobstream.plugintest;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.pluginbase.CfgSink;
import me.pood1e.jobstream.pluginbase.Mark;

@Slf4j
@Mark("test")
public class TestSink implements CfgSink<TestData, TestSinkConfig> {
    @Override
    public void sink(TestData input, TestSinkConfig config) {
        log.info("SINK {} : {}", config.getSinkLabel(), input.getValue());
    }
}
