package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import static javassist.bytecode.Opcode.*;

public class GLSLShader extends Mod {
    public GLSLShader() {
        name = "GLSL Shader";
        description = "Adds graphical shaders to the game.  Based on daxnitro's mod.";
        version = "1.0";

        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders.class");
    }
}
