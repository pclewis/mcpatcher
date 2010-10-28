package com.pclewis.mcpatcher;

public @interface ModInfo {
    String name();
    String description();
    String version();
    String author();
    boolean configurable() default false;
}
