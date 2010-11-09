package com.pclewis.mcpatcher.mods.hdfix;


import javassist.CtClass
import javassist.bytecode.ClassFile
import javassist.bytecode.CodeAttribute
import javassist.bytecode.ConstPool
import javassist.bytecode.MethodInfo
import com.pclewis.mcpatcher.*

@ModInfo(
        name="High Resolution Texture Fix",
        description="Fix for textures higher than 16x16 resolution.",
        author="xau <pcl@pclewis.com>",
        version="2.0"
)
public class HDFix extends Mod {
    @Override
    public String identifyClass(Deobfuscator de, CtClass ctClass) {
        ConstPool cp = ctClass.getClassFile().getConstPool();
        return identify(ctClass, [
            "animation.Manager": new MethodRef("org.lwjgl.opengl.GL11","glTexSubImage2D"),
            "render.Tessellator": "Not tesselating!",
            "render.Tool3D": -0.9375F,

            "animation.Texture": BytecodeBuilder.build(cp) {
                push 1024
                newarray ((byte)T_BYTE)
            },

            "animation.FlowLava": BytecodeBuilder.build(cp) {
                iconst_3
                idiv
                bipush byte, 16
                imul
                isub
                sipush 255
            },

            "animation.StillLava": BytecodeBuilder.build(cp) {
                i2f
                push float, Math.PI
                fmul
                fconst_2
                fmul
                push 16F
            },

            "animation.StillWater": BytecodeBuilder.build(cp) {
                invokestatic "java.lang.Math.random"
                push 0.05D
                dcmpg
                ifge 16
            },

            "animation.FlowWater": BytecodeBuilder.build(cp) {
                invokestatic "java.lang.Math.random"
                push 0.2D
                dcmpg
                ifge 16
            },

            "animation.Fire": BytecodeBuilder.build(cp) {
                invokestatic "java.lang.Math.random"
                invokestatic "java.lang.Math.random"
                dmul
                invokestatic "java.lang.Math.random"
                dmul
                push 4.0D
                dmul
                invokestatic "java.lang.Math.random"
            },

            "animation.Compass": BytecodeBuilder.build(cp) {
                push 90.0F
                fsub
                f2d
                push 3.141592653589793D // *NOT* PI (??)
                dmul
                push 180.0D
                ddiv
            }
        ]);
    }

    private String identify(CtClass ctClass, Map args) {
        ClassFile cf = ctClass.getClassFile();
        ConstPool cp = cf.getConstPool();

        def result = args.find { name, match ->
            if(match instanceof byte[]) {
                String codeToMatch = new String(match, "ISO-8859-1");
                for(Object mo : cf.getMethods()) {
                    MethodInfo mi = (MethodInfo) mo;
                    CodeAttribute ca = mi.getCodeAttribute();
                    if(ca == null)
                        continue;
                    String methodCode = new String(ca.getCode(), "ISO-8859-1");
                    if(methodCode.contains(codeToMatch))
                        return true;
                }
            } else {
                return ConstPoolUtils.contains(cp, match);
            }
        }

        return result ? result.key : null;
    }
}