package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.IOException;

/**
 * ClassPatch that adds a new method to a class file.  The added method is made public by default.
 */
abstract public class AddMethodPatch extends ClassPatch {
    private String name;
    private String type;
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
     * Create an AddMethodPatch with given name, type, and access flags.
     *
     * @param name        name of method
     * @param type        Java type descriptor of method; may use deobfuscated names
     * @param accessFlags method access flags
     * @see javassist.bytecode.AccessFlag
     */
    public AddMethodPatch(String name, String type, int accessFlags) {
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
    }

    /**
     * Create an AddMethodPatch with given name.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     *
     * @param name name of method
     */
    public AddMethodPatch(String name) {
        this(name, null);
    }

    /**
     * Create an AddMethodPatch with given name and access flags.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     *
     * @param name        name of method
     * @param accessFlags method access flags
     * @see javassist.bytecode.AccessFlag
     */
    public AddMethodPatch(String name, int accessFlags) {
        this(name, null, accessFlags);
    }

    @Override
    final public String getDescription() {
        return String.format("insert method %s %s", name, getDescriptor());
    }

    protected void prePatch(ClassFile classFile) throws BadBytecode, IOException {
    }

    @Override
    final public boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException, IOException {
        classMod.classFile = classFile;
        boolean patched = false;
        prePatch(classFile);
        ConstPool constPool = classFile.getConstPool();
        MethodRef methodRef = (MethodRef) classMod.getClassMap().map(new MethodRef(classMod.getDeobfClass(), name, getDescriptor()));
        MethodInfo methodInfo = new MethodInfo(constPool, methodRef.getName(), methodRef.getType());
        classMod.methodInfo = methodInfo;
        methodInfo.setAccessFlags(accessFlags);
        exceptionTable = new ExceptionTable(constPool);
        try {
            classMod.addToConstPool = true;
            classMod.resetLabels();
            byte[] code = generateMethod();
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
     * Called to get the descriptor for the new method.  Use this instead of setting the descriptor in the constructor
     * to have the method type set at patch time.
     *
     * @return descriptor
     */
    public String getDescriptor() {
        return type;
    }

    /**
     * @deprecated
     * @see #generateMethod()
     */
    public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws BadBytecode, IOException {
        throw new AbstractMethodError("generateMethod() unimplemented");
    }

    /**
     * Generate the bytecode for the new method.  May also set class members maxStackSize, numLocals,
     * and exceptionTable if the defaults are insufficient.
     *
     * @return bytecode
     * @throws BadBytecode
     * @throws IOException
     */
    public byte[] generateMethod() throws BadBytecode, IOException {
        return generateMethod(getClassFile(), getMethodInfo());
    }
}
