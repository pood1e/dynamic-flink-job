package me.pood1e.jobstream.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Operator<C> {
    private String id;
    private String name;
    private String description;
    private String impl;
    private C config;
}
