package me.pood1e.jobstream.common;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class Job implements Serializable {
    private String id;
    private String name;
    private String description;
    private List<String> functions;
    private List<Pipe> pipes;
    private long syncPeriod;
}
