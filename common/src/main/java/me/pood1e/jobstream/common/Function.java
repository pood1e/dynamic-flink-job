package me.pood1e.jobstream.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Function<C> {
    private String id;
    private String name;
    private int parallelism;
    private FunctionType type;
    private String description;
    private String impl;
    private long boundedOutOfOrder;
    private boolean keyBy;
    private long windowSeconds;
    private long rebalanceSeconds;
    private C config;
}
