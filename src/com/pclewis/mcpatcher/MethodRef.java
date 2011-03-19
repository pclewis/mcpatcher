package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

public class MethodRef extends JavaRef {
    public MethodRef(String className, String name, String type) {
        super(className, name, type);
    }

    @Override
    public boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_Methodref &&
            (className == null || constPool.getMethodrefClassName(tag).equals(className)) &&
            (name == null || constPool.getMethodrefName(tag).equals(name)) &&
            (type == null || constPool.getMethodrefType(tag).equals(type));
    }
}
