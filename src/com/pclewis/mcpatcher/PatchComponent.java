package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

interface PatchComponent {
    /**
     * Gets current javassist ClassFile object.
     *
     * @return class file
     */
    public ClassFile getClassFile();

    /**
     * Gets current javassist MethodInfo object.
     *
     * @return method info
     */
    public MethodInfo getMethodInfo();

    /**
     * Combines any number of inputs to form a binary regular expression.  Used by
     * getMatchExpression method.
     *
     * @param objects
     * @return regex
     */
    public String buildExpression(Object... objects);

    /**
     * Combines any number of inputs into a single byte array.  Used by getReplacementBytes method
     * to generate bytecode.
     *
     * @param objects
     * @return bytecode
     * @throws IOException
     */
    public byte[] buildCode(Object... objects) throws IOException;

    /**
     * Push a single constant (int, double, float, string) onto the stack.
     *
     * @param value
     * @return bytecode or regex
     */
    public Object push(Object value);

    /**
     * Invoke a method, field, or class reference with the given opcode.
     *
     * @param opcode
     * @param ref
     * @return bytecode
     */
    public byte[] reference(int opcode, JavaRef ref);

    /**
     * Gets a reference to the containing mod.
     *
     * @return mod
     */
    public Mod getMod();

    /**
     * Gets the mapping from descriptive class/field/method names to obfuscated names.
     *
     * @return mod ClassMap
     */
    public ClassMap getClassMap();

    /**
     * Convert method, field, or class reference to obfuscated names.
     *
     * @param ref
     * @return obfuscated reference
     */
    public JavaRef map(JavaRef ref);
}
