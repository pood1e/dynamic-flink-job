package me.pood1e.jobstream.plugintest2;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApiConfig {
    private List<ApiFetchConfig> fetchConfigs;
}
