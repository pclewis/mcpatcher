package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Base class for a reference to a class, method, or field.  Used to generate bytecode for
 * referencing classes and members.
 *
 * @see ClassMap#map(JavaRef)
 * @see ClassMod#reference(javassist.bytecode.MethodInfo, int, JavaRef)
 */
abstract public class JavaRef {
    protected String className;
    protected String name;
    protected String type;

    public JavaRef(String className, String name, String type) {
        this.className = (className == null ? null : className.replaceAll("/", "."));
        this.name = name;
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return String.format("%s{className='%s', name='%s', type='%s'}", getClass().getName(), className, name, type);
    }

    abstract boolean checkEqual(ConstPool constPool, int tag);
}
