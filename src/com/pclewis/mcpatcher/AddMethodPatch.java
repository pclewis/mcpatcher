package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.IOException;
import java.util.Arrays;

/**
 * ClassPatch that adds a new method to a class file.  The added method is made public by default.
 */
abstract public class AddMethodPatch extends ClassPatch {
    private MethodRef methodRef;
    private int accessFlags;
    private boolean allowDuplicate;

    /**
     * May be set to specify a different max stack size for the method.
     *
     * @see #generateMethod()
     */
    protected int maxStackSize;
    /**
     * May be set to specify a different number of local variables for the new method.
     *
     * @see #generateMethod()
     */
    protected int numLocals;
    /**
     * May be set to specify an exception table for the new method.
     *
     * @see #generateMethod()
     */
    protected ExceptionTable exceptionTable;

    /**
     * Add a new public, non-static method.
     *
     * @param methodRef method
     */
    public AddMethodPatch(MethodRef methodRef) {
        this(methodRef, AccessFlag.PUBLIC);
    }

    /**
     * Add a new method.
     *
     * @param methodRef   method
     * @param accessFlags Java access flags
     * @see javassist.bytecode.AccessFlag
     */
    public AddMethodPatch(MethodRef methodRef, int accessFlags) {
        this.methodRef = methodRef;
        this.accessFlags = accessFlags;
    }

    /**
     * Create an AddMethodPatch with given name and type.
     *
     * @param name name of method
     * @param type Java type descriptor of method; may use deobfuscated names
     * @see #AddMethodPatch(MethodRef)
     * @deprecated
     */
    public AddMethodPatch(String name, String type) {
        this(new MethodRef(null, name, type), AccessFlag.PUBLIC);
    }

    /**
     * Create an AddMethodPatch with given name, type, and access flags.
     *
     * @param name        name of method
     * @param type        Java type descriptor of method; may use deobfuscated names
     * @param accessFlags method access flags
     * @see #AddMethodPatch(MethodRef, int)
     * @see javassist.bytecode.AccessFlag
     * @deprecated
     */
    public AddMethodPatch(String name, String type, int accessFlags) {
        this(new MethodRef(null, name, type), accessFlags);
    }

    /**
     * Create an AddMethodPatch with given name.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     *
     * @param name name of method
     */
    public AddMethodPatch(String name) {
        this(name, AccessFlag.PUBLIC);
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
        this(new MethodRef(null, name, null), accessFlags);
    }

    /**
     * Set whether to ignore duplicate method errors when applying this patch.
     *
     * @param allowDuplicate true or false
     * @return this
     */
    public AddMethodPatch allowDuplicate(boolean allowDuplicate) {
        this.allowDuplicate = allowDuplicate;
        optional = allowDuplicate;
        return this;
    }

    @Override
    final public String getDescription() {
        return String.format("insert method %s %s", methodRef.getName(), getDescriptor());
    }

    protected void prePatch(ClassFile classFile) throws BadBytecode, IOException {
    }

    @Override
    final public boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException, IOException {
        boolean patched = false;
        prePatch(classFile);
        ConstPool constPool = classFile.getConstPool();
        MethodRef methodRef = (MethodRef) classMod.getClassMap().map(new MethodRef(classMod.getDeobfClass(), this.methodRef.getName(), getDescriptor()));
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
                int argLocals = (accessFlags & AccessFlag.STATIC) == 0 ? 0 : 1;
                int newMaxLocals = numLocals;
                for (String t : ConstPoolUtils.parseDescriptor(methodRef.getType())) {
                    if (t.equals("D") || t.equals("L")) {
                        argLocals += 2;
                    } else {
                        argLocals++;
                    }
                }
                newMaxLocals = Math.max(argLocals, newMaxLocals);
                newMaxLocals = Math.max(BytecodePatch.computeMaxLocals(codeAttribute), newMaxLocals);
                if ((accessFlags & AccessFlag.STATIC) == 0) {
                    newMaxLocals = Math.max(newMaxLocals, 1);
                }
                codeAttribute.setMaxLocals(newMaxLocals);
                int newStackSize = Math.max(codeAttribute.computeMaxStack(), maxStackSize);
                try {
                    classFile.addMethod(methodInfo);
                    patched = true;
                    recordPatch(String.format("stack size %d, local vars %d", newStackSize, newMaxLocals));
                } catch (DuplicateMemberException e) {
                    if (!allowDuplicate) {
                        boolean foundIdentical = false;
                        for (Object o : classFile.getMethods()) {
                            MethodInfo conflictMethod = (MethodInfo) o;
                            if (conflictMethod.getCodeAttribute() != null &&
                                conflictMethod.getName().equals(methodInfo.getName()) &&
                                conflictMethod.getDescriptor().equals(methodInfo.getDescriptor()) &&
                                conflictMethod.getAccessFlags() == methodInfo.getAccessFlags()) {
                                byte[] code1 = methodInfo.getCodeAttribute().getCode();
                                byte[] code2 = conflictMethod.getCodeAttribute().getCode();
                                if (Arrays.equals(code1, code2)) {
                                    foundIdentical = true;
                                    break;
                                }
                            }
                        }
                        if (!foundIdentical) {
                            throw e;
                        }
                    }
                }
            }
        } finally {
            classMod.methodInfo = null;
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
        return methodRef.getType();
    }

    /**
     * Generate the bytecode for the new method.  May also set class members maxStackSize, numLocals,
     * and exceptionTable if the defaults are insufficient.
     *
     * @return bytecode
     * @throws BadBytecode
     * @throws IOException
     */
    abstract public byte[] generateMethod() throws BadBytecode, IOException;
}
