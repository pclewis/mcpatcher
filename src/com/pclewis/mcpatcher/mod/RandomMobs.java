package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class RandomMobs extends Mod {
    public RandomMobs() {
        name = MCPatcherUtils.RANDOM_MOBS;
        author = "Balthichou";
        description = "Randomize mob skins. Based on Balthichou's mod.";
        version = "1.0";
        defaultEnabled = false;

        addDependency(MCPatcherUtils.HD_TEXTURES);

        classMods.add(new RenderLivingMod());
        classMods.add(new EntityMod());
        classMods.add(new EntityLivingMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS));
    }

    private class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        FCONST_0,
                        push(methodInfo, -24.0f),
                        BytecodeMatcher.anyFLOAD,
                        FMUL,
                        push(methodInfo, 0.0078125f),
                        FSUB,
                        FCONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glTranslatef", "(FFF)V"))
                    );
                }
            }.setMethodName("doRenderLiving"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace mob textures";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.capture(BytecodeMatcher.anyALOAD),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityLiving", "getEntityTexture", "()Ljava/lang/String;"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntity;)Ljava/lang/String;"))
                    );
                }
            });
        }
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            classSignatures.add(new ConstSignature("Pos"));
            classSignatures.add(new ConstSignature("Motion"));
            classSignatures.add(new ConstSignature("Rotation"));

            memberMappers.add(new FieldMapper("entityId", "I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
            memberMappers.add(new MethodMapper("getEntityTexture", "()Ljava/lang/String;"));
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            parentClass = "Entity";

            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("Health"));
        }
    }
}
