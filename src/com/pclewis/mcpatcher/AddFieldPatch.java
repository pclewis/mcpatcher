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
     * @param name field name
     * @param type field type descriptor
     * @param accessFlags Java access flags (public, private, etc.).
     * @see javassist.bytecode.AccessFlag
     */
    public AddFieldPatch(String name, String type, int accessFlags) {
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
    }

    @Override
    public String getDescription() {
        return String.format("insert field %s %s", name, type);
    }

    @Override
    boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException {
        FieldInfo fieldInfo = new FieldInfo(classFile.getConstPool(), name, type);
        fieldInfo.setAccessFlags(accessFlags);
        recordPatch();
        classFile.addField(fieldInfo);
        return true;
    }
}
