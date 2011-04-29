package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;

/**
 * ClassSignature that matches a particular bytecode sequence.
 */
abstract public class BytecodeSignature extends ClassSignature {
    /**
     * Optional method name.
     */
    protected String methodName = null;
    /**
     * Matcher object.
     *
     * @see BytecodeMatcher
     */
    protected BytecodeMatcher matcher;

    /**
     * Generate a regular expression for the current method.
     *
     * @param methodInfo method object used with push and reference calls
     * @return String regex
     * @see ClassSignature#push(javassist.bytecode.MethodInfo, Object)
     * @see ClassSignature#reference(javassist.bytecode.MethodInfo, int, JavaRef)
     */
    abstract public String getMatchExpression(MethodInfo methodInfo);

    protected boolean match(MethodInfo methodInfo) {
        matcher = new BytecodeMatcher(getMatchExpression(methodInfo));
        return matcher.match(methodInfo);
    }

    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        for (Object o : classFile.getMethods()) {
            MethodInfo methodInfo = (MethodInfo) o;
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            if (codeAttribute != null && match(methodInfo)) {
                if (methodName != null) {
                    String deobfName = classMod.getDeobfClass();
                    tempClassMap.addClassMap(deobfName, ClassMap.filenameToClassName(filename));
                    tempClassMap.addMethodMap(deobfName, methodName, methodInfo.getName());
                }
                afterMatch(classFile, methodInfo);
                return true;
            }
        }
        return false;
    }

    /**
     * Assigns a name to a signature.  On matching, the target class and method will be added.
     * to the class map.
     *
     * @param methodName descriptive name of method
     * @return this
     */
    public BytecodeSignature setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * Called immediately after a successful match.  Gives an opportunity to extract bytecode
     * values using getCaptureGroup, for example.
     *
     * @param classFile  matched class file
     * @param methodInfo matched method
     */
    public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
    }
}
