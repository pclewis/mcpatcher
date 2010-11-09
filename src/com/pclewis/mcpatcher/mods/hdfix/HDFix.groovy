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
        return identify(de, ctClass, [
            "animation.Manager": new MethodRef("org.lwjgl.opengl.GL11","glTexSubImage2D"),
            "render.Tessellator": "Not tesselating!",
            "render.Tool3D": -0.9375F,

            "animation.Texture": BytecodeBuilder.build(cp) {
                push 1024
                newarray ((byte)T_BYTE)
            },

            "animation.FlowLava": {
                superclass("animation.Texture") &&
                containsCode {
                    iconst_3
                    idiv
                    push 16
                    imul
                    isub
                    push 255
                }
            },

            "animation.StillLava": {
                superclass("animation.Texture") &&
                containsCode {
                    i2f
                    push float, Math.PI
                    fmul
                    fconst_2
                    fmul
                    push 16F
                }
            },

            "animation.StillWater": {
                superclass("animation.Texture") &&
                containsCode {
                    invokestatic "java.lang.Math.random"
                    push 0.05D
                    dcmpg
                    ifge 16
                }
            },

            "animation.FlowWater": {
                superclass("animation.Texture") &&
                containsCode {
                    invokestatic "java.lang.Math.random"
                    push 0.2D
                    dcmpg
                    ifge 16
                }
            },

            "animation.Fire": {
                superclass("animation.Texture") &&
                containsCode {
                    invokestatic "java.lang.Math.random"
                    invokestatic "java.lang.Math.random"
                    dmul
                    invokestatic "java.lang.Math.random"
                    dmul
                    push 4.0D
                    dmul
                    invokestatic "java.lang.Math.random"
                }
            },

            "animation.Compass": {
                superclass("animation.Texture") &&
                containsCode {
                    push 90.0F
                    fsub
                    f2d
                    push 3.141592653589793D // *NOT* PI (??)
                    dmul
                    push 180.0D
                    ddiv
                }
            }
        ]);
    }

    private String identify(Deobfuscator de, CtClass ctClass, Map args) {
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
            } else if (match instanceof Closure) {
                match.setDelegate(new IdentifierContext(de, cp, cf));
                return match();
            } else {
                return ConstPoolUtils.contains(cp, match);
            }
        }

        return result ? result.key : null;
    }

    private static class IdentifierContext {
        Deobfuscator de;
        ConstPool constPool;
        ClassFile classFile;

        public IdentifierContext(Deobfuscator de, ConstPool cp, ClassFile cf) {
            this.de = de;
            this.constPool = cp;
            this.classFile = cf;
        }

        public boolean containsCode(Closure c) {
            byte[] match = BytecodeBuilder.build(constPool, c);
            String codeToMatch = new String(match, "ISO-8859-1");
            for(Object mo : classFile.getMethods()) {
                MethodInfo mi = (MethodInfo) mo;
                CodeAttribute ca = mi.getCodeAttribute();
                if(ca == null)
                    continue;
                String methodCode = new String(ca.getCode(), "ISO-8859-1");
                if(methodCode.contains(codeToMatch))
                    return true;
            }
            return false;
        }

        public boolean superclass(String className) {
            String superName = classFile.getSuperclass();
            return (superName==className || (de.hasClass(className) && superName==de.getClassName(className)) );
        }
    }
}