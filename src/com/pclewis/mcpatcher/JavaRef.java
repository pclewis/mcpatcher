package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;

/**
 * Base class for a reference to a class, method, or field.  Used to generate bytecode for
 * referencing classes and members.
 *
 * @see ClassMap#map(JavaRef)
 * @see ClassMod#reference(int, JavaRef)
 */
abstract public class JavaRef implements Comparable<JavaRef> {
    final protected String className;
    final protected String name;
    final protected String type;

    public JavaRef(String className, String name, String type) {
        this.className = (className == null ? null : className.replaceAll("/", "."));
        this.name = name;
        this.type = type;
        if (type != null) {
            ConstPoolUtils.checkTypeDescriptorSyntax(type);
        }
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

    private static int compareString(String a, String b) {
        if (a == null) {
            return b == null ? 0 : -1;
        } else {
            return b == null ? 1 : a.compareTo(b);
        }
    }

    public int compareTo(JavaRef that) {
        int c = compareString(className, that.className);
        if (c != 0) {
            return c;
        }
        c = compareString(name, that.name);
        if (c != 0) {
            return c;
        }
        c = compareString(type, that.type);
        if (c != 0) {
            return c;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JavaRef && compareTo((JavaRef) o) == 0;
    }

    @Override
    public int hashCode() {
        return (className == null ? 0 : className.hashCode()) +
            (name == null ? 0 : name.hashCode()) +
            (type == null ? 0 : type.hashCode());
    }
}
