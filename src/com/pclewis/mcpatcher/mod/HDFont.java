package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class HDFont extends Mod {
    private int imageWidthRegister;
    private int imageHeightRegister;

    public HDFont() {
        name = "HD Font";
        author = "MCPatcher";
        description = "Provides support for higher resolution fonts.";
        version = "1.0";

        classMods.add(new FontRendererMod());
    }

    private static class FontRendererMod extends ClassMod {
        public FontRendererMod() {
            classSignatures.add(new FixedBytecodeSignature(
                DCONST_0,
                DCONST_0,
                DCONST_0,
                ILOAD
            ));
            classSignatures.add(new FixedBytecodeSignature(
                ALOAD, BinaryRegex.any(),
                ICONST_0,
                ICONST_0,
                ILOAD, BinaryRegex.capture(BinaryRegex.any()), // capture group 1
                ILOAD, BinaryRegex.capture(BinaryRegex.any()), // capture group 2
                ALOAD, BinaryRegex.any(),
                ICONST_0,
                ILOAD, BinaryRegex.backReference(1),
                INVOKEVIRTUAL
            ) {
                @Override
                public void afterMatch(ClassFile classFile) {
                    HDFont mod = (HDFont) getMod();
                    mod.imageWidthRegister = matcher.getCaptureGroup(1)[0];
                    mod.imageHeightRegister = matcher.getCaptureGroup(2)[0];
                    Logger.log(Logger.LOG_CONST, "font registers = %d, %d",
                        mod.imageWidthRegister, mod.imageHeightRegister
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "font width 7 -> i / 16 - 1";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 7),
                        ISTORE
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ILOAD, ((HDFont) getMod()).imageWidthRegister,
                        push(methodInfo, 16),
                        IDIV,
                        ICONST_1,
                        ISUB,
                        ISTORE
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "font width 8 -> i / 16";
                }

                @Override
                public String getMatchExpression(MethodInfo mi) {
                    return buildExpression(
                        BinaryRegex.lookBehind(BinaryRegex.subset(new byte[]{(byte) IREM, (byte) IDIV}, true), false),
                        push(mi, 8),
                        BinaryRegex.lookAhead(BinaryRegex.subset(new byte[]{(byte) IMUL, (byte) IF_ICMPGE}, true), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo mi) throws IOException {
                    return buildCode(
                        ILOAD, ((HDFont) getMod()).imageWidthRegister,
                        push(mi, 16),
                        IDIV
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "font whitespace width 2 -> i / 64";
                }

                @Override
                public String getMatchExpression(MethodInfo mi) {
                    return buildExpression(
                        ICONST_2,
                        ISTORE
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo mi) throws IOException {
                    return buildCode(
                        ILOAD, ((HDFont) getMod()).imageWidthRegister,
                        push(mi, 64),
                        IDIV,
                        ISTORE
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "font charWidth computation: j + 2 -> (128 * j + 256) / i";
                }

                @Override
                public String getMatchExpression(MethodInfo mi) {
                    return buildExpression(
                        ILOAD, BinaryRegex.capture(BinaryRegex.any()),
                        ICONST_2,
                        IADD,
                        IASTORE
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo mi) throws IOException {
                    return buildCode(
                        push(mi, 128),
                        ILOAD, getCaptureGroup(1),
                        IMUL,
                        push(mi, 256),
                        IADD,
                        ILOAD, ((HDFont) getMod()).imageWidthRegister,
                        IDIV,
                        IASTORE
                    );
                }
            });
        }
    }
}
