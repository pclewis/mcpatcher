package com.pclewis.mcpatcher;

import javassist.bytecode.MethodInfo;

import java.io.IOException;

interface PatchComponent {
    /**
     * Set a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name
     * @param value
     */
    public void setModParam(String name, Object value);

    /**
     * Get a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name
     * @return parameter value or ""
     */
    public String getModParam(String name);

    /**
     * Get a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name
     * @return parameter value or 0
     */
    public int getModParamInt(String name);

    /**
     * Get a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name
     * @return parameter value or false
     */
    public boolean getModParamBool(String name);

    /**
     * @return mod ClassMap
     */
    public ClassMap getClassMap();

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
     * Convert method, field, or class reference to obfuscated names.
     *
     * @param ref
     * @return obfuscated reference
     */
    public JavaRef map(JavaRef ref);

    /**
     * Push a single constant (int, double, float, string) onto the stack.
     *
     * @param methodInfo
     * @param value
     * @return bytecode
     */
    public byte[] push(MethodInfo methodInfo, Object value);

    /**
     * Invoke a method, field, or class reference with the given opcode.
     *
     * @param methodInfo
     * @param opcode
     * @param ref
     * @return bytecode
     */
    public byte[] reference(MethodInfo methodInfo, int opcode, JavaRef ref);
}
