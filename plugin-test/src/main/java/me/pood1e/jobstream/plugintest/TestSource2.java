package me.pood1e.jobstream.plugintest;

import me.pood1e.jobstream.pluginbase.CfgSource;
import me.pood1e.jobstream.pluginbase.Mark;
import me.pood1e.jobstream.pluginbase.MarkSourceType;
import me.pood1e.jobstream.pluginbase.SourceThreadType;

import java.util.List;

@Mark("test")
@MarkSourceType(SourceThreadType.MULTI_PERIOD)
public class TestSource2 implements CfgSource<TestData, TestSourceConfig, TestSourceFetchConfig> {
    @Override
    public TestData fetch(TestSourceFetchConfig config) {
        return TestData.builder().value(config.getValue() + "aaa;s").timestamp(System.currentTimeMillis()).build();
    }

    @Override
    public List<TestSourceFetchConfig> getFetchConfigs(TestSourceConfig testSourceConfig) {
        return testSourceConfig.getConfigs();
    }
}
