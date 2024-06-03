package me.pood1e.jobstream.plugintest2;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String body;
    private Map<String, List<String>> headers;
    private int statusCode;
}
