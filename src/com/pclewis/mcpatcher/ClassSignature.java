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
    protected ClassMod classMod;

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

    abstract public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap);

    /**
     * Called immediately after a successful match.  Gives an opportunity to extract bytecode
     * values using getCaptureGroup, for example.
     *
     * @param classFile matched class file
     */
    protected void afterMatch(ClassFile classFile) {
    }

    void setClassMod(ClassMod classMod) {
        this.classMod = classMod;
    }

    // PatchComponent methods
    
    final public ClassFile getClassFile() {
        return classMod.getClassFile();
    }
    
    final public MethodInfo getMethodInfo() {
        return classMod.getMethodInfo();
    }

    final public String buildExpression(Object... objects) {
        return classMod.buildExpression(objects);
    }

    final public byte[] buildCode(Object... objects) throws IOException {
        return classMod.buildCode(objects);
    }
    
    final public Object push(Object value) {
        return classMod.push(value);
    }

    /**
     * @deprecated
     */
    final public Object push(MethodInfo methodInfo, Object value) {
        return classMod.push(methodInfo, value);
    }

    final public byte[] reference(int opcode, JavaRef ref) {
        return classMod.reference(opcode, ref);
    }

    /**
     * @deprecated
     */
    final public byte[] reference(MethodInfo methodInfo, int opcode, JavaRef ref) {
        return classMod.reference(methodInfo, opcode, ref);
    }

    final public Mod getMod() {
        return classMod.getMod();
    }

    final public ClassMap getClassMap() {
        return classMod.getClassMap();
    }

    final public JavaRef map(JavaRef ref) {
        return classMod.map(ref);
    }
}
