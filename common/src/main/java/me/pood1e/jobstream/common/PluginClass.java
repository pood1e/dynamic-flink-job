package me.pood1e.jobstream.common;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginClass {
    private String name;
    private String className;
    private FunctionType type;
}
