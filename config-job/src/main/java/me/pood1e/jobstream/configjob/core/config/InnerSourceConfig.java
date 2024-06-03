package me.pood1e.jobstream.configjob.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InnerSourceConfig extends ExternalConfig {
    private long syncPeriod;
    private String jobId;
}
