package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

public interface ConstChanger {
	int index();
	String description();
	int add(ConstPool cp);
}
