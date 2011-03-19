package com.pclewis.mcpatcher;

import javassist.bytecode.*;

/**
 * BinaryMatcher used with Java bytecode.
 */
public class BytecodeMatcher extends BinaryMatcher {
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
