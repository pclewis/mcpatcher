package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import static javassist.bytecode.Opcode.*;

/**
 * BinaryMatcher used with Java bytecode.
 */
public class BytecodeMatcher extends BinaryMatcher {
    /**
     * Fixed regex that matches any ALOAD ... instruction.
     */
    public static final String anyALOAD = BinaryRegex.or(
        BinaryRegex.build(ALOAD_0),
        BinaryRegex.build(ALOAD_1),
        BinaryRegex.build(ALOAD_2),
        BinaryRegex.build(ALOAD_3),
        BinaryRegex.build(ALOAD, BinaryRegex.any()),
        BinaryRegex.build(WIDE, ALOAD, BinaryRegex.any(2))
    );

    /**
     * Fixed regex that matches any ASTORE ... instruction.
     */
    public static final String anyASTORE = BinaryRegex.or(
        BinaryRegex.build(ASTORE_0),
        BinaryRegex.build(ASTORE_1),
        BinaryRegex.build(ASTORE_2),
        BinaryRegex.build(ASTORE_3),
        BinaryRegex.build(ASTORE, BinaryRegex.any()),
        BinaryRegex.build(WIDE, ASTORE, BinaryRegex.any(2))
    );

    /**
     * Fixed regex that matches any ILOAD ... instruction.
     */
    public static final String anyILOAD = BinaryRegex.or(
        BinaryRegex.build(ILOAD_0),
        BinaryRegex.build(ILOAD_1),
        BinaryRegex.build(ILOAD_2),
        BinaryRegex.build(ILOAD_3),
        BinaryRegex.build(ILOAD, BinaryRegex.any()),
        BinaryRegex.build(WIDE, ILOAD, BinaryRegex.any(2))
    );

    /**
     * Fixed regex that matches any ISTORE ... instruction.
     */
    public static final String anyISTORE = BinaryRegex.or(
        BinaryRegex.build(ISTORE_0),
        BinaryRegex.build(ISTORE_1),
        BinaryRegex.build(ISTORE_2),
        BinaryRegex.build(ISTORE_3),
        BinaryRegex.build(ISTORE, BinaryRegex.any()),
        BinaryRegex.build(WIDE, ISTORE, BinaryRegex.any(2))
    );

    /**
     * Fixed regex that matches any FLOAD ... instruction.
     */
    public static final String anyFLOAD = BinaryRegex.or(
        BinaryRegex.build(FLOAD_0),
        BinaryRegex.build(FLOAD_1),
        BinaryRegex.build(FLOAD_2),
        BinaryRegex.build(FLOAD_3),
        BinaryRegex.build(FLOAD, BinaryRegex.any()),
        BinaryRegex.build(WIDE, FLOAD, BinaryRegex.any(2))
    );

    /**
     * Fixed regex that matches any FSTORE ... instruction.
     */
    public static final String anyFSTORE = BinaryRegex.or(
        BinaryRegex.build(FSTORE_0),
        BinaryRegex.build(FSTORE_1),
        BinaryRegex.build(FSTORE_2),
        BinaryRegex.build(FSTORE_3),
        BinaryRegex.build(FSTORE, BinaryRegex.any()),
        BinaryRegex.build(WIDE, FSTORE, BinaryRegex.any(2))
    );

    /**
     * Fixed regex that matches any LDC/LDC_W ... instruction.
     */
    public static final String anyLDC = BinaryRegex.or(
        BinaryRegex.build(LDC, BinaryRegex.any()),
        BinaryRegex.build(LDC_W, BinaryRegex.any(2))
    );

    /**
     * Regex that matches any opcode+16-bit const pool index
     *
     * @param opcode opcode, e.g., INVOKEVIRTUAL
     * @return
     */
    public static String anyReference(int opcode) {
        return BinaryRegex.build(opcode, BinaryRegex.any(opcode == INVOKEINTERFACE ? 4 : 2));
    }

    /**
     * Construct a new matcher for the given regular expression.
     *
     * @param objects BinaryRegex elements that make up the expression to match
     * @see BinaryRegex#build(Object...)
     */
    public BytecodeMatcher(Object... objects) {
        super(objects);
    }

    /**
     * Match expression against any method in a class file.
     *
     * @param classFile class file to match
     * @return true if any method matches
     */
    public boolean match(ClassFile classFile) {
        for (Object o : classFile.getMethods()) {
            MethodInfo mi = (MethodInfo) o;
            if (match(mi)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match expression against a single method in a class file.
     *
     * @param methodInfo method to match
     * @return true if match
     */
    public boolean match(MethodInfo methodInfo) {
        return match(methodInfo, 0);
    }

    /**
     * Match expression against a single method in a class file, starting at a particular offset.
     *
     * @param methodInfo method to match
     * @param offset     position in bytecode at which to start looking for matches.
     * @return true if match
     */
    public boolean match(MethodInfo methodInfo, int offset) {
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        byte[] code = ca.getCode();
        int next;

        while (ci.hasNext() && offset < code.length) {
            if (!match(code, offset)) {
                break;
            }
            try {
                while ((next = ci.next()) < getStart())
                    ;
            } catch (BadBytecode e) {
                break;
            }
            if (next == getStart()) {
                return true;
            }
            offset = next;
        }

        return false;
    }
}
