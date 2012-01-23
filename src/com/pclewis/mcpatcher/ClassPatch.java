package com.pclewis.mcpatcher;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.MethodInfo;

import java.io.IOException;
import java.util.HashMap;

/**
 * Base class of all class patches.
 */
abstract public class ClassPatch implements PatchComponent {
    ClassMod classMod;
    HashMap<String, Integer> numMatches = new HashMap<String, Integer>();

    /**
     * Returns a text description of the patch.  This string will be displayed in the log window and
     * in the patch summary.
     *
     * @return String description
     */
    abstract public String getDescription();

    /**
     * Applies patch to a class file.
     *
     * @param classFile target class file
     * @return true if changes were made
     * @throws BadBytecode              propagated by javassist.bytecode
     * @throws DuplicateMemberException propagated by javassist.bytecode
     * @throws IOException              if an error occurs while writing new bytecode
     */
    abstract boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException, IOException;

    void setClassMod(ClassMod classMod) {
        this.classMod = classMod;
    }

    private void addPatch(String desc) {
        int val = 0;
        if (numMatches.containsKey(desc)) {
            val = numMatches.get(desc);
        }
        numMatches.put(desc, val + 1);
    }

    /**
     * Writes a patch to the log and adds it to the patch summary.
     */
    protected void recordPatch() {
        String desc = getDescription();
        addPatch(desc);
        Logger.log(Logger.LOG_PATCH, "%s", desc);
    }

    /**
     * Writes a patch to the log and adds it to the patch summary.
     *
     * @param extra additional text to add to the log output
     */
    protected void recordPatch(String extra) {
        String desc = getDescription();
        addPatch(desc);
        Logger.log(Logger.LOG_PATCH, "%s %s", desc, extra);
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
