package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class RandomMobs extends Mod {
    public RandomMobs(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.RANDOM_MOBS;
        author = "Balthichou";
        description = "Randomize mob skins if texture pack supports it. Based on Balthichou's mod.";
        version = "1.1";

        classMods.add(new RenderLivingMod());
        classMods.add(new RenderEyesMod("Spider"));
        if (minecraftVersion.compareTo(MinecraftVersion.parseVersion("Minecraft Beta 1.8 Prerelease 1")) >= 0) {
            classMods.add(new RenderEyesMod("Enderman"));
        }
        classMods.add(new EntityMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new MinecraftMod());
        classMods.add(new HDTexture.TexturePackListMod(false));
        classMods.add(new HDTexture.TexturePackBaseMod());

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

    private class RenderEyesMod extends ClassMod {
        private String mobName;
        private String eyeTexture;

        public RenderEyesMod(String mob) {
            mobName = mob;
            eyeTexture = "/mob/" + mobName.toLowerCase() + "_eyes.png";

            classSignatures.add(new ConstSignature(eyeTexture));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + mobName.toLowerCase() + " eye texture";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, eyeTexture)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_1,
                        reference(methodInfo, GETFIELD, new FieldRef("Entity", "entityId", "I")),
                        push(methodInfo, eyeTexture),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(ILjava/lang/String;)Ljava/lang/String;"))
                    );
                }
            });
        }

        @Override
        public String getDeobfClass() {
            return "Render" + mobName;
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

    private class MinecraftMod extends ClassMod {
        MinecraftMod() {
            classSignatures.add(new FilenameSignature("net/minecraft/client/Minecraft.class"));

            memberMappers.add(new FieldMapper("texturePackList", "LTexturePackList;"));
        }
    }
}
