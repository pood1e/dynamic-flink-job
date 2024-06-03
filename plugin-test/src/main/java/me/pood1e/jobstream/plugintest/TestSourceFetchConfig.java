package me.pood1e.jobstream.plugintest;

import lombok.Getter;
import lombok.Setter;
import me.pood1e.jobstream.pluginbase.PeriodicalConfig;

@Getter
@Setter
public class TestSourceFetchConfig implements PeriodicalConfig {
    private String id;
    private String value;
    private long periodSeconds;
}
