package me.pood1e.jobstream.plugintest2;

import lombok.Getter;
import lombok.Setter;
import me.pood1e.jobstream.pluginbase.PeriodicalConfig;

import java.util.Map;

@Getter
@Setter
public class ApiFetchConfig implements PeriodicalConfig {
    private String id;
    private long periodSeconds;
    private String url;
    private Map<String, String> headers;
    private String method;
    private String body;
}
