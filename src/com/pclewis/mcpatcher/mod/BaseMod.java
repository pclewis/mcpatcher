package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.ClassMap;
import com.pclewis.mcpatcher.MCPatcher;
import com.pclewis.mcpatcher.Mod;

public class BaseMod extends Mod {
    public BaseMod() {
        name = "__Base";
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcher.UTILS_CLASS));
    }
}
