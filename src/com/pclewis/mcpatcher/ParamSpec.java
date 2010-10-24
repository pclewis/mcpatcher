package com.pclewis.mcpatcher;

public class ParamSpec {
    String name;
    String defaultSource;
    String description;

    public ParamSpec(String name, String defaultSource, String description) {
        this.name = name;
        this.defaultSource = defaultSource;
        this.description = description;
    }
}
