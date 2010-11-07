package com.pclewis.mcpatcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value= RetentionPolicy.RUNTIME)
public @interface ModInfo {
    String name();
    String description();
    String version();
    String author();
    boolean configurable() default false;
}
