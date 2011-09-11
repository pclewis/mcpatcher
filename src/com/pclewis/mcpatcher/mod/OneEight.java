package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class OneEight extends Mod {
    public OneEight() {
        name = MCPatcherUtils.ONE_EIGHT;
        author = "MCPatcher";
        description = "Fixes furnace crash bug in 1.8 pre-release.  Based on idshift's zz.class patch.";
        version = "1.0";

        classMods.add(new FurnaceMod());
    }

    private static class FurnaceMod extends ClassMod {
        public FurnaceMod() {
            classSignatures.add(new ConstSignature("Furnace"));
            classSignatures.add(new ConstSignature("BurnTime"));
            classSignatures.add(new ConstSignature("CookTime"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "Fix crash when placing items into furnace";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals("()V")) {
                        return buildExpression(
                            BinaryRegex.lookBehind(BinaryRegex.build(
                                ALOAD_0,
                                DUP,
                                GETFIELD, BinaryRegex.any(2),
                                ICONST_1,
                                ISUB,
                                PUTFIELD, BinaryRegex.any(2),
                                ALOAD_0,
                                GETFIELD, BinaryRegex.any(2)
                            ), true),
                            BinaryRegex.capture(BinaryRegex.any(1, 240)),
                            BinaryRegex.capture(BinaryRegex.build(
                                ILOAD_2,
                                IFEQ, BinaryRegex.any(2),
                                ALOAD_0,
                                INVOKEVIRTUAL, BinaryRegex.any(2),
                                RETURN
                            )),
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        DUP,
                        IFNONNULL, branch("A"),
                        POP,
                        GOTO, branch("B"),

                        label("A"),
                        getCaptureGroup(1),

                        label("B"),
                        getCaptureGroup(2)
                    );
                }
            });
        }
    }
}
