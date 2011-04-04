package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.MethodInfo;

import java.io.IOException;
import java.io.InputStream;

import static javassist.bytecode.Opcode.*;

public class BaseMod extends Mod {
    public BaseMod() {
        name = "__Base";
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";

        classMods.add(new LDC_WMod());

        filesToAdd.add(MCPatcher.UTILS_CLASS + ".class");
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        return BaseMod.class.getResourceAsStream("/" + name);
    }

    private static class LDC_WMod extends ClassMod {
        public LDC_WMod() {
            global = true;

            classSignatures.add(new FixedBytecodeSignature(
                LDC_W, 0
            ));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "LDC_W 0x00 -> LDC";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        LDC_W, 0, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        LDC, getCaptureGroup(1)
                    );
                }
            });
        }
    }
}
