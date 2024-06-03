package me.pood1e.jobstream.plugintest;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestData {
    private String value;
    private long timestamp;
}
