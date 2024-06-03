package me.pood1e.jobstream.plugintest;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TestProcessorConfig {
    private String label;
    private Map<String, String> side;
}
