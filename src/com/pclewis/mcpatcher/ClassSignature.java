package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

/**
 * Base class of all class file signatures.  Used to select which .class file(s) a ClassMod
 * should target
 */
abstract public class ClassSignature implements PatchComponent {
    boolean negate = false;
    private ClassMod classMod;

    /**
     * Negates a signature's meaning.  A class that does not match underlying signature will be
     * accepted and vice versa.
     *
     * @param negate
     * @return this
     */
    public ClassSignature negate(boolean negate) {
        this.negate = negate;
        return this;
    }

    /**
     * Called immediately after a successful match.  Gives an opportunity to extract bytecode
     * values using getCaptureGroup, for example.
     *
     * @param classFile matched class file
     */
    protected void afterMatch(ClassFile classFile) {
    }

    final void setClassMod(ClassMod classMod) {
        this.classMod = classMod;
    }

    // PatchComponent methods

    /**
     * @see Mod#classMap
     */
    final public ClassMap getClassMap() {
        return classMod.getClassMap();
    }

    /**
     * @see ClassMod#buildExpression(Object...)
     */
    final public String buildExpression(Object... objects) {
        return classMod.buildExpression(objects);
    }

    /**
     * @see ClassMod#buildCode(Object...)
     */
    final public byte[] buildCode(Object... objects) throws IOException {
        return classMod.buildCode(objects);
    }

    /**
     * @see ClassMap#map(JavaRef)
     */
    final public JavaRef map(JavaRef ref) {
        return classMod.map(ref);
    }

    /**
     * @see ClassMod#push(javassist.bytecode.MethodInfo, Object)
     */
    final public byte[] push(MethodInfo methodInfo, Object value) {
        return classMod.push(methodInfo, value);
    }

    /**
     * @see ClassMod#reference(javassist.bytecode.MethodInfo, int, JavaRef)
     */
    final public byte[] reference(MethodInfo methodInfo, int opcode, JavaRef ref) {
        return classMod.reference(methodInfo, opcode, ref);
    }

    /**
     * @see Mod#setModParam(String, Object)
     */
    final public void setModParam(String name, Object value) {
        classMod.setModParam(name, value);
    }

    /**
     * @see Mod#getModParam(String)
     */
    final public String getModParam(String name) {
        return classMod.getModParam(name);
    }

    /**
     * @see Mod#getModParamInt(String)
     */
    final public int getModParamInt(String name) {
        return classMod.getModParamInt(name);
    }

    /**
     * @see Mod#getModParamBool(String)
     */
    final public boolean getModParamBool(String name) {
        return classMod.getModParamBool(name);
    }
}
