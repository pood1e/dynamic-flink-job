package me.pood1e.jobstream.plugintest;


import me.pood1e.jobstream.pluginbase.CfgProcessor;
import me.pood1e.jobstream.pluginbase.Mark;

@Mark("test")
public class TestProcessor implements CfgProcessor<TestData, TestData, TestProcessorConfig> {

    @Override
    public void process(TestData testData, TestProcessorConfig config, Collector<TestData> collector) {
        String mark = config.getLabel();
        collector.collect(generateTestData(testData, mark));
        if (config.getSide() != null) {
            config.getSide().forEach((key, value) -> collector.side(key, generateTestData(testData, value)));
        }
    }

    private TestData generateTestData(TestData testData, String mark) {
        String value = testData.getValue() + " (" + mark + ")";
        return TestData.builder().value(value).timestamp(testData.getTimestamp()).build();
    }
}
