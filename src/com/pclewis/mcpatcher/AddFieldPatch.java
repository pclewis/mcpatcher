package com.pclewis.mcpatcher;

import javassist.bytecode.*;

/**
 * ClassPatch that adds a new field to a class file.
 */
public class AddFieldPatch extends ClassPatch {
    private String name;
    private String type;
    private int accessFlags;

    /**
     * Add a new public, non-static field.
     *
     * @param name field name
     * @param type field type descriptor
     */
    public AddFieldPatch(String name, String type) {
        this(name, type, AccessFlag.PUBLIC);
    }

    /**
     * Add a new field.
     *
     * @param name        field name
     * @param type        field type descriptor
     * @param accessFlags Java access flags (public, private, etc.).
     * @see javassist.bytecode.AccessFlag
     */
    public AddFieldPatch(String name, String type, int accessFlags) {
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
    }

    /**
     * Add a new public, non-static field.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     *
     * @param name field name
     */
    public AddFieldPatch(String name) {
        this(name, null, AccessFlag.PUBLIC);
    }

    /**
     * Add a new field.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     *
     * @param name        field name
     * @param accessFlags Java access flags (public, private, etc.).
     * @see javassist.bytecode.AccessFlag
     */
    public AddFieldPatch(String name, int accessFlags) {
        this(name, null, accessFlags);
    }

    /**
     * Called to get the descriptor for the new field.  Use this instead of setting the descriptor in the constructor
     * to have the field type set at patch time.
     *
     * @return descriptor
     */
    public String getDescriptor() {
        return type;
    }

    @Override
    public String getDescription() {
        return String.format("insert field %s %s", name, getDescriptor());
    }

    @Override
    boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException {
        classMod.classFile = classFile;
        classMod.methodInfo = null;
        FieldInfo fieldInfo = new FieldInfo(classFile.getConstPool(), name, classMod.getClassMap().mapTypeString(getDescriptor()));
        fieldInfo.setAccessFlags(accessFlags);
        recordPatch();
        classFile.addField(fieldInfo);
        return true;
    }
}
