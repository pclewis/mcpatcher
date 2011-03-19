package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to a field within a class.
 *
 * @see JavaRef
 */
public class FieldRef extends JavaRef {
    public FieldRef(String className, String name, String type) {
        super(className, name, type);
    }

    @Override
    boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_Fieldref &&
            (className == null || constPool.getFieldrefClassName(tag).equals(className)) &&
            (name == null || constPool.getFieldrefName(tag).equals(name)) &&
            (type == null || constPool.getFieldrefType(tag).equals(type));
    }
}
