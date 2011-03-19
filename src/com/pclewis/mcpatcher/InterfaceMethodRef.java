package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to an interface method.
 *
 * @see JavaRef
 */
public class InterfaceMethodRef extends JavaRef {
    public InterfaceMethodRef(String className, String name, String type) {
        super(className, name, type);
    }

    @Override
    boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_InterfaceMethodref &&
            (className == null || constPool.getInterfaceMethodrefClassName(tag).equals(className)) &&
            (name == null || constPool.getInterfaceMethodrefName(tag).equals(name)) &&
            (type == null || constPool.getInterfaceMethodrefType(tag).equals(type));
    }
}
