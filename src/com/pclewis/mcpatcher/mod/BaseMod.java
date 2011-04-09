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

        filesToAdd.add(MCPatcher.UTILS_CLASS + ".class");
    }
}
