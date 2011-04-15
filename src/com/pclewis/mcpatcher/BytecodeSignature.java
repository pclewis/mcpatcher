package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

/**
 * ClassSignature that matches a particular bytecode sequence.
 */
abstract public class BytecodeSignature extends ClassSignature {
    String methodName = null;
    /**
     * Matcher object
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

    /**
     * Match against the given method.  If successful, matcher contains further
     * match information.
     *
     * @param methodInfo current method
     * @return true if match
     * @see #matcher
     */
    public boolean match(MethodInfo methodInfo) {
        matcher = new BytecodeMatcher(getMatchExpression(methodInfo));
        return matcher.match(methodInfo);
    }

    /**
     * Gets descriptive name assigned to the signature.
     *
     * @return String name
     * @see #setMethodName(String)
     */
    public String getMethodName() {
        return methodName;
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
     * @param classFile matched class file
     * @param methodInfo matched method
     */
    public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
    }
}
