package me.pood1e.jobstream.plugintest;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestSourceConfig {
    private List<TestSourceFetchConfig> configs;
}
