package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.IOException;

/**
 * ClassPatch that adds a new method to a class file.  The added method is made public by default.
 */
abstract public class AddMethodPatch extends ClassPatch {
    /**
     * Name of method.
     */
    protected String name;
    /**
     * Java type descriptor of method; may use deobfuscated names.
     */
    protected String type;
    private int accessFlags;
    /**
     * May be set to specify a different max stack size for the method.
     *
     * @see #generateMethod(ClassFile, MethodInfo)
     */
    protected int maxStackSize;
    /**
     * May be set to specify a different number of local variables for the new method.
     *
     * @see #generateMethod(ClassFile, MethodInfo)
     */
    protected int numLocals;
    /**
     * May be set to specify an exception table for the new method.
     *
     * @see #generateMethod(ClassFile, MethodInfo)
     */
    protected ExceptionTable exceptionTable;

    /**
     * Create an AddMethodPatch with given name and type.
     *
     * @param name name of method
     * @param type Java type descriptor of method; may use deobfuscated names
     */
    public AddMethodPatch(String name, String type) {
        this(name, type, AccessFlag.PUBLIC);
    }

    /**
     * Create an AddMethodPatch with given name and type.
     *
     * @param name name of method
     * @param type Java type descriptor of method; may use deobfuscated names
     * @param accessFlags method access flags
     */
    public AddMethodPatch(String name, String type, int accessFlags) {
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
    }

    @Override
    final public String getDescription() {
        return String.format("insert method %s %s", name, type);
    }

    protected void prePatch(ClassFile classFile) throws BadBytecode, IOException {
    }

    @Override
    final public boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException, IOException {
        boolean patched = false;
        prePatch(classFile);
        ConstPool constPool = classFile.getConstPool();
        MethodInfo methodInfo = new MethodInfo(constPool, name, classMod.getClassMap().mapTypeString(type));
        methodInfo.setAccessFlags(accessFlags);
        exceptionTable = new ExceptionTable(constPool);
        try {
            classMod.addToConstPool = true;
            classMod.resetLabels();
            byte[] code = generateMethod(classFile, methodInfo);
            if (code != null) {
                classMod.resolveLabels(code, 0, 0);
                CodeAttribute codeAttribute = new CodeAttribute(constPool, maxStackSize, numLocals, code, exceptionTable);
                methodInfo.setCodeAttribute(codeAttribute);
                int newMaxLocals = Math.max(BytecodePatch.computeMaxLocals(codeAttribute), numLocals);
                codeAttribute.setMaxLocals(newMaxLocals);
                int newStackSize = Math.max(codeAttribute.computeMaxStack(), maxStackSize);
                recordPatch(String.format("stack size %d, local vars %d", newStackSize, newMaxLocals));
                classFile.addMethod(methodInfo);
                patched = true;
            }
        } finally {
            classMod.addToConstPool = false;
        }
        return patched;
    }

    /**
     * Generate the bytecode for the new method.  May also set class members maxStackSize, numLocals,
     * and exceptionTable if the defaults are insufficient.
     *
     * @param classFile  target class file
     * @param methodInfo new empty method
     * @return bytecode
     * @throws BadBytecode
     * @throws IOException
     */
    abstract public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws BadBytecode, IOException;
}
