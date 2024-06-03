package me.pood1e.jobstream.plugintest2;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.pluginbase.CfgSink;

@Slf4j
public class ApiSink implements CfgSink<ApiResponse, ApiSinkConfig> {
    @Override
    public void sink(ApiResponse input, ApiSinkConfig config) {
        log.info(input.getBody());
    }
}
