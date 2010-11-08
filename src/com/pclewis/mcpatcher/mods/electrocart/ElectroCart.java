package com.pclewis.mcpatcher.mods.electrocart;

import com.pclewis.mcpatcher.*;
import javassist.CtClass;
import javassist.bytecode.ConstPool;

@ModInfo(
    name        = "ElectroCart",
    description = "Control minecarts with redstone.",
    version     = "1.0",
    author      = "xau <pcl@pclewis.com>"
)
public class ElectroCart extends Mod {
    @Override
    public String identifyClass(Deobfuscator de, CtClass ctClass) {
        if(de.getMinecraftVersion().equalTo("Alpha 1.1.2_01")) {
            if(ctClass.getName().equals("kf"))
                return "blocks.Wire";
            if(ctClass.getName().equals("jo"))
                return "items.Minecart";
            if(ctClass.getName().equals("oc"))
                return "entities.Minecart";
        }

        ConstPool cp = ctClass.getClassFile().getConstPool();
        if(ConstPoolUtils.contains(cp, "Skipping Entity with id "))
            return "EntityList";

        return null;
    }
}
