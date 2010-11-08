package com.pclewis.mcpatcher;

public class MethodRef {
	String className;
	String name;
	String type;

    public String getClassName() {
        return className;
    }

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public MethodRef(String className, String name, String type) {
		this.className = className;
		this.name = name;
		this.type = type;
	}

    public MethodRef(String className, String name) {
        this(className, name, null);
    }

	public String toString() {
		return "MethodRef{" +
			"className='" + className + '\'' +
			", name='" + name + '\'' +
			", type='" + type + '\'' +
			'}';
	}
}
