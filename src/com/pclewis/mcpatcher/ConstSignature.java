package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

/**
 * ClassSignature that matches if the class's constant pool contains the given value or reference.
 */
public class ConstSignature extends ClassSignature {
    private Object value;
    private int tag;

    /**
     * Constructor
     *
     * @param value can be a constant (float, double, String) or JavaRef
     */
    public ConstSignature(Object value) {
        this.value = value;
        tag = ConstPoolUtils.getTag(value);
    }

    @Override
    boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        ConstPool cp = classFile.getConstPool();
        for (int i = 1; i < cp.getSize(); i++) {
            if (match(cp, i)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(ConstPool cp, int index) {
        return cp.getTag(index) == tag && ConstPoolUtils.checkEqual(cp, index, value);
    }
}
