package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.IOException;
import java.util.ArrayList;

import static javassist.bytecode.Opcode.*;

/**
 * ClassPatch that matches a replaces and specific bytecode sequence.  The full power of Java
 * regular expressions is available.  Wildcards, capturing, and backreferences can be used via
 * their wrappers in the BinaryRegex class.
 *
 * @see BinaryRegex
 */
abstract public class BytecodePatch extends ClassPatch {
    BytecodeMatcher matcher;
    MethodRef targetMethod;
    int labelOffset;

    public BytecodePatch targetMethod(MethodRef targetMethod) {
        this.targetMethod = targetMethod;
        return this;
    }

    /**
     * Can be overridden to skip certain methods during patching.
     *
     * @param methodInfo current method
     * @return true if method should be considered for patching
     */
    public boolean filterMethod(MethodInfo methodInfo) {
        if (targetMethod == null) {
            return true;
        } else {
            JavaRef ref = map(targetMethod);
            return methodInfo.getDescriptor().equals(ref.getType()) && methodInfo.getName().equals(ref.getName());
        }
    }

    /**
     * Get a regular expression to look for within the target method's bytecode.  The expression
     * can contain opcodes (IADD, IF_ICMPGE, etc.).  Use the push method to generate bytecode
     * for loading constants, getting/setting fields, and invoking methods.
     *
     * @param methodInfo current method
     * @return string using BinaryRegex methods
     */
    abstract public String getMatchExpression(MethodInfo methodInfo);

    /**
     * Get replacement bytecode after a match.  May be shorter or longer than the original bytecode;
     * MCPatcher will take care of padding.  Use the buildCode method to generate the return byte
     * array.
     *
     * @param methodInfo current method
     * @return replacement bytes
     * @throws IOException
     */
    abstract public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException;

    boolean apply(MethodInfo mi) throws BadBytecode {
        boolean patched = false;
        CodeAttribute ca = mi.getCodeAttribute();
        if (ca == null) {
            return patched;
        }
        matcher = new BytecodeMatcher(getMatchExpression(mi));
        CodeIterator ci = ca.iterator();
        int oldStackSize = ca.computeMaxStack();
        int oldMaxLocals = ca.getMaxLocals();
        int offset = 0;
        ArrayList<String> txtBefore = null;

        while (matcher.match(mi, offset)) {
            recordPatch(String.format("%s%s@%d", mi.getName(), mi.getDescriptor(), matcher.getStart()));

            byte repl[];
            try {
                classMod.addToConstPool = true;
                classMod.resetLabels();
                labelOffset = 0;
                repl = getReplacementBytes(mi);
            } catch (IOException e) {
                Logger.log(e);
                repl = null;
            } finally {
                classMod.addToConstPool = false;
            }
            if (repl == null) {
                offset = ci.next();
                continue;
            }

            if (Logger.isLogLevel(Logger.LOG_BYTECODE)) {
                txtBefore = bytecodeToString(ca, matcher.getStart(), matcher.getEnd());
            }
            int gap = repl.length - matcher.getMatchLength();
            int skip = 0;
            if (gap > 0) {
                skip = ci.insertGap(matcher.getStart(), gap) - gap;
            } else if (gap < 0) {
                skip = -gap;
                gap = 0;
            }
            for (int i = 0; i < skip; i++) {
                ci.writeByte(Opcode.NOP, matcher.getStart() + i);
            }
            classMod.resolveLabels(repl, matcher.getStart() + skip, labelOffset);
            ci.write(repl, matcher.getStart() + skip);
            offset = matcher.getStart() + repl.length + skip;
            if (Logger.isLogLevel(Logger.LOG_BYTECODE)) {
                ArrayList<String> txtAfter = bytecodeToString(ca, matcher.getStart(), offset);
                logBytecodePatch(txtBefore, txtAfter);
            }
            patched = true;
            ci.move(offset);
        }

        if (patched) {
            int newStackSize = ca.computeMaxStack();
            if (oldStackSize < newStackSize) {
                Logger.log(Logger.LOG_METHOD, "increasing stack size from %d to %d", oldStackSize, newStackSize);
                ca.setMaxStack(newStackSize);
            }
            int newMaxLocals = computeMaxLocals(ca);
            if (oldMaxLocals < newMaxLocals) {
                Logger.log(Logger.LOG_METHOD, "increasing max locals from %d to %d", oldMaxLocals, newMaxLocals);
                ca.setMaxLocals(newMaxLocals);
            }
        }

        return patched;
    }

    static int computeMaxLocals(CodeAttribute ca) throws BadBytecode {
        CodeIterator ci = ca.iterator();
        int maxLocals = 0;
        while (ci.hasNext()) {
            int offset = ci.next();
            int local;
            switch (ci.byteAt(offset)) {
                case ALOAD_0:
                case ILOAD_0:
                case LLOAD_0:
                case FLOAD_0:
                case DLOAD_0:
                case ASTORE_0:
                case ISTORE_0:
                case LSTORE_0:
                case FSTORE_0:
                case DSTORE_0:
                    local = 0;
                    break;

                case ALOAD_1:
                case ILOAD_1:
                case LLOAD_1:
                case FLOAD_1:
                case DLOAD_1:
                case ASTORE_1:
                case ISTORE_1:
                case LSTORE_1:
                case FSTORE_1:
                case DSTORE_1:
                    local = 1;
                    break;

                case ALOAD_2:
                case ILOAD_2:
                case LLOAD_2:
                case FLOAD_2:
                case DLOAD_2:
                case ASTORE_2:
                case ISTORE_2:
                case LSTORE_2:
                case FSTORE_2:
                case DSTORE_2:
                    local = 2;
                    break;

                case ALOAD_3:
                case ILOAD_3:
                case LLOAD_3:
                case FLOAD_3:
                case DLOAD_3:
                case ASTORE_3:
                case ISTORE_3:
                case LSTORE_3:
                case FSTORE_3:
                case DSTORE_3:
                    local = 3;
                    break;

                case ALOAD:
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                case ASTORE:
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                    local = ci.byteAt(offset + 1) & 0xff;
                    break;

                case WIDE:
                    switch (ci.byteAt(++offset)) {
                        case ALOAD:
                        case ILOAD:
                        case LLOAD:
                        case FLOAD:
                        case DLOAD:
                        case ASTORE:
                        case ISTORE:
                        case LSTORE:
                        case FSTORE:
                        case DSTORE:
                            local = Util.demarshal(new byte[]{(byte) ci.byteAt(offset + 1), (byte) ci.byteAt(offset + 2)});
                            break;

                        default:
                            continue;
                    }
                    break;

                default:
                    continue;
            }
            maxLocals = Math.max(maxLocals, local + 1);
        }
        return maxLocals;
    }

    @Override
    boolean apply(ClassFile classFile) throws BadBytecode {
        boolean patched = false;
        for (Object o : classFile.getMethods()) {
            MethodInfo mi = (MethodInfo) o;
            if (filterMethod(mi)) {
                if (apply(mi)) {
                    patched = true;
                }
            }
        }
        return patched;
    }

    private static ArrayList<String> bytecodeToString(CodeAttribute ca, int start, int end) {
        ArrayList<String> as = new ArrayList<String>();
        CodeIterator ci = ca.iterator();
        try {
            ci.move(start);
            int pos = ci.next();
            while (pos < end && ci.hasNext()) {
                int next = ci.next();
                String s = Mnemonic.OPCODE[ci.byteAt(pos++)].toUpperCase();
                for (; pos < next; pos++) {
                    s += String.format(" 0x%02x", (int) ci.byteAt(pos));
                }
                as.add(s);
            }
        } catch (Exception e) {
            as.add(e.toString());
        }
        return as;
    }

    private static void logBytecodePatch(ArrayList<String> before, ArrayList<String> after) {
        final String format = "%-24s  %s";
        int max = Math.max(before.size(), after.size());
        for (int i = 0; i < max; i++) {
            Logger.log(Logger.LOG_BYTECODE, format,
                (i < before.size() ? before.get(i) : ""),
                (i < after.size() ? after.get(i) : "")
            );
        }
    }

    /**
     * Get a captured subexpression after a match.  Can only be called in getReplacementBytes.
     *
     * @param group number of capture group, starting at 1
     * @return byte array
     */
    final protected byte[] getCaptureGroup(int group) {
        return matcher.getCaptureGroup(group);
    }

    /**
     * Get matching bytecode string after a match.  Can only be called in getReplacementBytes.
     *
     * @return byte array
     */
    final protected byte[] getMatch() {
        return matcher.getMatch();
    }

    /**
     * BytecodePatch that inserts code after a match.
     */
    abstract public static class InsertAfter extends BytecodePatch {
        @Override
        final public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
            byte[] insertBytes = getInsertBytes(methodInfo);
            if (insertBytes == null) {
                return null;
            } else {
                labelOffset = matcher.getMatchLength();
                return buildCode(
                    matcher.getMatch(),
                    insertBytes
                );
            }
        }

        abstract public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException;
    }

    /**
     * BytecodePatch that inserts code before a match.
     */
    abstract public static class InsertBefore extends BytecodePatch {
        @Override
        final public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
            byte[] insertBytes = getInsertBytes(methodInfo);
            if (insertBytes == null) {
                return null;
            } else {
                return buildCode(
                    insertBytes,
                    matcher.getMatch()
                );
            }
        }

        abstract public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException;
    }
}
