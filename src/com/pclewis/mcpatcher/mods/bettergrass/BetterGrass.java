package com.pclewis.mcpatcher.mods.bettergrass;

import com.pclewis.mcpatcher.*;
import javassist.CtClass;
import javassist.bytecode.ConstPool;

@ModInfo(
    name        = "Better Grass",
    description = "Show grass on sides of grass blocks connected to grass below.",
    version     = "0.9",
    author      = "MrMessiah, xau <pcl@pclewis.com>"
)
public class BetterGrass extends Mod {
    @Override
    public String identifyClass(Deobfuscator de, CtClass ctClass) {
        ConstPool cp = ctClass.getClassFile().getConstPool();
        if(ConstPoolUtils.contains(cp, " is already occupied by "))
            return "Block";
        return null;
    }

    private static class BGPatch extends Patch {
        public String getDescription() { return "Better Grass"; }
        public void visitClassPre(CtClass ct) throws Exception {
            ct.getConstructor("(I)V").insertAfter("this.bb = 0;");
            ct.getMethod("a", "(Lnm;IIII)I").setBody("");
        }
    }
}
