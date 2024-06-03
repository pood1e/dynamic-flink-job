package me.pood1e.jobstream.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Pipe implements Serializable {
    private String source;
    private String target;
    private boolean broadcast;
    private String key;
}
