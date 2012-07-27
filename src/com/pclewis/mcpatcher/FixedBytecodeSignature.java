package com.pclewis.mcpatcher;

import javassist.bytecode.MethodInfo;

/**
 * Fixed BytecodeSignature that does not require any method information.  Offers better
 * performance when the target bytecode sequence does not contain any references.
 */
public class FixedBytecodeSignature extends BytecodeSignature {
    /**
     * @param objects BinaryRegex expressions representing a fixed signature
     * @see BinaryRegex#build(Object...)
     */
    public FixedBytecodeSignature(Object... objects) {
        matcher = new BytecodeMatcher(objects);
    }

    @Override
    final public String getMatchExpression() {
        throw new AssertionError("Unreachable");
    }

    @Override
    void initMatcher() {
    }
}
