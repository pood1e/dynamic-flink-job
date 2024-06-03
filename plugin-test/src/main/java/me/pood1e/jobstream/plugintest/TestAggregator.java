package me.pood1e.jobstream.plugintest;

import me.pood1e.jobstream.pluginbase.CfgAggregator;

public class TestAggregator implements CfgAggregator<TestData, TestAggData, TestAggData, TestAggConfig> {

    @Override
    public TestAggData createAccumulator(TestAggConfig config) {
        return new TestAggData();
    }

    @Override
    public TestAggData add(TestData testData, TestAggData accumulator, TestAggConfig config) {
        accumulator.setCount(accumulator.getCount() + testData.getValue().length());
        return accumulator;
    }

    @Override
    public TestAggData getResult(TestAggData testAggData, TestAggConfig config) {
        return testAggData;
    }

    @Override
    public TestAggData merge(TestAggData a1, TestAggData a2, TestAggConfig config) {
        a1.setCount(a1.getCount() + a2.getCount());
        return a1;
    }

    @Override
    public long getWatermark(TestData testData, long recordTime, TestAggConfig config) {
        return testData.getTimestamp();
    }
}
