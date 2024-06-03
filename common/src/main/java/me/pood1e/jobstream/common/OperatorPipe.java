package me.pood1e.jobstream.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperatorPipe {
    private String id;
    private String source;
    private String target;
    private String key;
}
