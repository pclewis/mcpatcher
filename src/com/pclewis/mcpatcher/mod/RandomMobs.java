package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;

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
        version = "1.2";

        classMods.add(new RenderLivingMod());
        classMods.add(new RenderEyesMod("Spider"));
        if (minecraftVersion.compareTo("Beta 1.8 Prerelease 1") >= 0) {
            classMods.add(new RenderEyesMod("Enderman"));
        }
        classMods.add(new EntityMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new NBTTagCompoundMod());
        if (minecraftVersion.compareTo("Beta 1.9") >= 0) {
            classMods.add(new RenderSnowmanMod());
            classMods.add(new RenderMooshroomMod());
        }
        classMods.add(new BaseMod.MinecraftMod().mapTexturePackList());
        classMods.add(new BaseMod.TexturePackListMod(minecraftVersion));
        classMods.add(new BaseMod.TexturePackBaseMod(minecraftVersion));

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.MOB_OVERLAY_CLASS));
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V"))
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

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "entityId", "I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "nextEntityID", "I"))
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getEntityTexture", "()Ljava/lang/String;")));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "writeToNBT", "(LNBTTagCompound;)V"), new MethodRef(getDeobfClass(), "readFromNBT", "(LNBTTagCompound;)V"))
                .accessFlag(AccessFlag.PUBLIC, true)
            );

            patches.add(new AddFieldPatch(new FieldRef(getDeobfClass(), ENTITY_SKIN_FIELD, "J")));
            patches.add(new AddFieldPatch(new FieldRef(getDeobfClass(), ENTITY_SKIN_SET_FIELD, "Z")));

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

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getLong", "(Ljava/lang/String;)J")));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "setLong", "(Ljava/lang/String;J)V")));
        }
    }

    private class RenderSnowmanMod extends ClassMod {
        RenderSnowmanMod() {
            parentClass = "RenderLiving";

            final MethodRef renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntitySnowman;F)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f = 0.625f;
                        push(0.625f),
                        BytecodeMatcher.anyFSTORE,

                        // GL11.glTranslatef(0.0f, -0.34375f, 0.0f);
                        push(0.0f),
                        push(-0.34375f),
                        push(0.0f),
                        reference(INVOKESTATIC, glTranslatef)
                    );
                }
            }.setMethod(renderEquippedItems));
        }
    }

    private class RenderMooshroomMod extends ClassMod {
        RenderMooshroomMod() {
            parentClass = "RenderLiving";

            final FieldRef renderBlocks = new FieldRef(getDeobfClass(), "renderBlocks", "LRenderBlocks;");
            final FieldRef mushroomRed = new FieldRef("Block", "mushroomRed", "LBlockFlower;");
            final MethodRef renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntityMooshroom;F)V");
            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(Ljava/lang/String;)V");
            final MethodRef glEnable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glEnable", "(I)V");
            final MethodRef glPushMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPushMatrix", "()V");
            final MethodRef renderBlockAsItem = new MethodRef("RenderBlocks", "renderBlockAsItem", "(LBlock;IF)V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // loadTexture("/terrain.png");
                        ALOAD_0,
                        push("/terrain.png"),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),

                        // GL11.glEnable(GL11.GL_CULL_FACE);
                        push(2884),
                        reference(INVOKESTATIC, glEnable),

                        // GL11.glPushMatrix();
                        reference(INVOKESTATIC, glPushMatrix),

                        // ...
                        BinaryRegex.any(0, 100),

                        // renderBlocks.renderBlockAsItem(Block.mushroomRed, 0, 1.0f);
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETSTATIC),
                        push(0),
                        push(1.0f),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(renderEquippedItems)
                .addXref(1, loadTexture)
                .addXref(2, renderBlocks)
                .addXref(3, mushroomRed)
                .addXref(4, renderBlockAsItem)
            );

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up custom mooshroom overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/terrain.png")
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_1,
                        push("/terrain.png"),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "setupMooshroom", "(LEntity;Ljava/lang/String;)Ljava/lang/String;"))
                    );
                }
            }.targetMethod(renderEquippedItems));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render mooshroom overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, renderBlocks),
                        reference(GETSTATIC, mushroomRed),
                        push(0),
                        push(1.0f),
                        reference(INVOKEVIRTUAL, renderBlockAsItem)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderMooshroomOverlay", "()Z")),
                        IFNE, branch("A"),
                        getMatch(),
                        label("A")
                    );
                }
            }.targetMethod(renderEquippedItems));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "finish mooshroom overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "finish", "()V"))
                    );
                }
            }.targetMethod(renderEquippedItems));
        }
    }
}
