package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class RandomMobs extends Mod {
    private static final String ENTITY_SKIN_FIELD = "randomMobsSkin";
    private static final String ENTITY_SKIN_SET_FIELD = "randomMobsSkinSet";

    public RandomMobs(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.RANDOM_MOBS;
        author = "Balthichou";
        description = "Randomize mob skins if texture pack supports it. Based on Balthichou's mod.";
        website = "http://www.minecraftforum.net/topic/244172-";
        version = "1.1";

        classMods.add(new RenderLivingMod());
        classMods.add(new RenderEyesMod("Spider"));
        if (minecraftVersion.compareTo("Beta 1.8 Prerelease 1") >= 0) {
            classMods.add(new RenderEyesMod("Enderman"));
        }
        classMods.add(new EntityMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new NBTTagCompoundMod());
        classMods.add(new BaseMod.MinecraftMod().mapTexturePackList());
        classMods.add(new BaseMod.TexturePackListMod());
        classMods.add(new BaseMod.TexturePackBaseMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS));
    }

    private class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FCONST_0,
                        push(-24.0f),
                        BytecodeMatcher.anyFLOAD,
                        FMUL,
                        push(0.0078125f),
                        FSUB,
                        FCONST_0,
                        reference(INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glTranslatef", "(FFF)V"))
                    );
                }
            }.setMethodName("doRenderLiving"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace mob textures";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BytecodeMatcher.anyALOAD),
                        reference(INVOKEVIRTUAL, new MethodRef("EntityLiving", "getEntityTexture", "()Ljava/lang/String;"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntity;)Ljava/lang/String;"))
                    );
                }
            });
        }
    }

    private class RenderEyesMod extends ClassMod {
        private String mobName;
        private String eyeTexture;

        RenderEyesMod(String mob) {
            mobName = mob;
            eyeTexture = "/mob/" + mobName.toLowerCase() + "_eyes.png";

            classSignatures.add(new ConstSignature(eyeTexture));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + mobName.toLowerCase() + " eye texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(eyeTexture)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_1,
                        push(eyeTexture),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntity;Ljava/lang/String;)Ljava/lang/String;"))
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
            memberMappers.add(new FieldMapper("nextEntityID", "I")
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
            memberMappers.add(new MethodMapper("getEntityTexture", "()Ljava/lang/String;"));
            memberMappers.add(new MethodMapper(new String[]{"writeToNBT", "readFromNBT"}, "(LNBTTagCompound;)V")
                .accessFlag(AccessFlag.PUBLIC, true)
            );

            patches.add(new AddFieldPatch(ENTITY_SKIN_FIELD, "J"));
            patches.add(new AddFieldPatch(ENTITY_SKIN_SET_FIELD, "Z"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "write skin to nbt";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // nbttagcompound.setLong(skin);
                        ALOAD_1,
                        push(ENTITY_SKIN_FIELD),
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef("Entity", ENTITY_SKIN_FIELD, "J")),
                        reference(INVOKEVIRTUAL, new MethodRef("NBTTagCompound", "setLong", "(Ljava/lang/String;J)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "writeToNBT", "(LNBTTagCompound;)V")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "read skin from nbt";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // skin = nbttagcompound.getLong("skin");
                        ALOAD_0,
                        ALOAD_1,
                        push(ENTITY_SKIN_FIELD),
                        reference(INVOKEVIRTUAL, new MethodRef("NBTTagCompound", "getLong", "(Ljava/lang/String;)J")),
                        reference(PUTFIELD, new FieldRef("Entity", ENTITY_SKIN_FIELD, "J")),

                        // if (skin != 0L) {
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef("Entity", ENTITY_SKIN_FIELD, "J")),
                        LCONST_0,
                        LCMP,
                        IFEQ, branch("A"),

                        // skinSet = true;
                        ALOAD_0,
                        ICONST_1,
                        reference(PUTFIELD, new FieldRef("Entity", ENTITY_SKIN_SET_FIELD, "Z")),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "readFromNBT", "(LNBTTagCompound;)V")));
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            parentClass = "Entity";

            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("Health"));
        }
    }

    private class NBTTagCompoundMod extends ClassMod {
        NBTTagCompoundMod() {
            classSignatures.add(new ConstSignature(new ClassRef("java.util.HashMap")));
            classSignatures.add(new ConstSignature(" entries"));

            memberMappers.add(new MethodMapper("getLong", "(Ljava/lang/String;)J"));
            memberMappers.add(new MethodMapper("setLong", "(Ljava/lang/String;J)V"));
        }
    }
}
