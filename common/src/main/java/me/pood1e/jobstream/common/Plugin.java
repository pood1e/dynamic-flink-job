package me.pood1e.jobstream.common;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plugin {
    private String id;
    private String name;
    private String jarHash;
    private List<String> packages;
    private List<PluginClass> classes;
}
