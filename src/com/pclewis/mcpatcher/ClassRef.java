package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Reference to a class.
 *
 * @see JavaRef
 */
public class ClassRef extends JavaRef {
    /**
     * @param className name of class
     */
    public ClassRef(String className) {
        super(className, null, null);
    }

    @Override
    boolean checkEqual(ConstPool constPool, int tag) {
        return constPool.getTag(tag) == ConstPool.CONST_Class &&
            constPool.getClassInfo(tag).equals(className);
    }
}
