package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class RandomMobs extends Mod {
    private static final String EXTRA_INFO_CLASS = MCPatcherUtils.RANDOM_MOBS_CLASS + "$ExtraInfo";

    public RandomMobs(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.RANDOM_MOBS;
        author = "Balthichou";
        description = "Randomize mob skins if texture pack supports it. Based on Balthichou's mod.";
        website = "http://www.minecraftforum.net/topic/244172-";
        version = "1.4";

        addDependency(BaseTexturePackMod.NAME);

        classMods.add(new RenderLivingMod());
        classMods.add(new RenderEyesMod("Spider"));
        if (minecraftVersion.compareTo("Beta 1.8 Prerelease 1") >= 0) {
            classMods.add(new RenderEyesMod("Enderman"));
        }
        classMods.add(new EntityMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new NBTTagCompoundMod());
        if (minecraftVersion.compareTo("Beta 1.9") >= 0) {
            classMods.add(new BaseMod.TessellatorMod(minecraftVersion));
            classMods.add(new RenderMod());
            classMods.add(new RenderSnowmanMod());
            classMods.add(new RenderMooshroomMod());
        }
        classMods.add(new MiscSkinMod("RenderSheep", "/mob/sheep_fur.png"));
        classMods.add(new MiscSkinMod("RenderWolf", "/mob/wolf_collar.png"));

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS + "$1"));
        filesToAdd.add(ClassMap.classNameToFilename(EXTRA_INFO_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS + "$MobEntry"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.RANDOM_MOBS_CLASS + "$SkinEntry"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.MOB_OVERLAY_CLASS));
    }

    private class RenderMod extends ClassMod {
        RenderMod() {
            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("%clamp%/misc/shadow.png"));
            classSignatures.add(new ConstSignature(15.99f));

            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(Ljava/lang/String;)V");

            memberMappers.add(new MethodMapper(loadTexture)
                .accessFlag(AccessFlag.PROTECTED, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            parentClass = "Render";

            classSignatures.add(new ConstSignature(180.0f));

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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntityLiving;)Ljava/lang/String;"))
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntityLiving;Ljava/lang/String;)Ljava/lang/String;"))
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

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),

                        // prevPosX = posX = d;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_1,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // prevPosY = posY = d1;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_3,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // prevPosZ = posZ = d2;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD, 5,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "setPositionAndRotation", "(DDDFF)V"))
                .addXref(1, new FieldRef(getDeobfClass(), "posX", "D"))
                .addXref(2, new FieldRef(getDeobfClass(), "prevPosX", "D"))
                .addXref(3, new FieldRef(getDeobfClass(), "posY", "D"))
                .addXref(4, new FieldRef(getDeobfClass(), "prevPosY", "D"))
                .addXref(5, new FieldRef(getDeobfClass(), "posZ", "D"))
                .addXref(6, new FieldRef(getDeobfClass(), "prevPosZ", "D"))
            );

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "entityId", "I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "nextEntityID", "I"))
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            final FieldRef info = new FieldRef(getDeobfClass(), "randomMobsInfo", "L" + EXTRA_INFO_CLASS + ";");
            final MethodRef getEntityTexture = new MethodRef(getDeobfClass(), "getEntityTexture", "()Ljava/lang/String;");
            final MethodRef writeToNBT = new MethodRef(getDeobfClass(), "writeToNBT", "(LNBTTagCompound;)V");
            final MethodRef readFromNBT = new MethodRef(getDeobfClass(), "readFromNBT", "(LNBTTagCompound;)V");
            final MethodRef getLong = new MethodRef("NBTTagCompound", "getLong", "(Ljava/lang/String;)J");
            final MethodRef setLong = new MethodRef("NBTTagCompound", "setLong", "(Ljava/lang/String;J)V");
            final MethodRef getInteger = new MethodRef("NBTTagCompound", "getInteger", "(Ljava/lang/String;)I");
            final MethodRef setInteger = new MethodRef("NBTTagCompound", "setInteger", "(Ljava/lang/String;I)V");

            parentClass = "Entity";

            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("Health"));

            memberMappers.add(new MethodMapper(getEntityTexture));
            memberMappers.add(new MethodMapper(writeToNBT, readFromNBT)
                .accessFlag(AccessFlag.PUBLIC, true)
            );

            patches.add(new AddFieldPatch(info));

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
                        // MobRandomizer.ExtraInfo.writeToNBT(this, nbttagcompound);
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(EXTRA_INFO_CLASS, "writeToNBT", "(LEntityLiving;LNBTTagCompound;)V"))
                    );
                }
            }.targetMethod(writeToNBT));

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
                        // MobRandomizer.ExtraInfo.readFromNBT(this, nbttagcompound);
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(EXTRA_INFO_CLASS, "readFromNBT", "(LEntityLiving;LNBTTagCompound;)V"))
                    );
                }
            }.targetMethod(readFromNBT));
        }
    }

    private class NBTTagCompoundMod extends ClassMod {
        NBTTagCompoundMod() {
            classSignatures.add(new ConstSignature(new ClassRef("java.util.HashMap")));
            classSignatures.add(new ConstSignature(" entries"));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getLong", "(Ljava/lang/String;)J")));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "setLong", "(Ljava/lang/String;J)V")));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getInteger", "(Ljava/lang/String;)I")));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "setInteger", "(Ljava/lang/String;I)V")));
        }
    }

    private class RenderSnowmanMod extends ClassMod {
        RenderSnowmanMod() {
            parentClass = "RenderLiving";

            final MethodRef renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntitySnowman;F)V");
            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(Ljava/lang/String;)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
            final FieldRef snowmanOverlayTexture = new FieldRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "snowmanOverlayTexture", "Ljava/lang/String;");
            final MethodRef setupSnowman = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "setupSnowman", "(LEntityLiving;)Z");
            final MethodRef renderSnowmanOverlay = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderSnowmanOverlay", "()V");

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

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render snowman overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // renderManager.itemRenderer.renderItem(par1EntitySnowman, itemstack, 0);
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
                        BytecodeMatcher.anyReference(GETFIELD),
                        ALOAD_1,
                        ALOAD_3,
                        ICONST_0,
                        BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (setupSnowman(entityLiving)) {
                        ALOAD_1,
                        reference(INVOKESTATIC, setupSnowman),
                        IFEQ, branch("A"),

                        // loadTexture(MobOverlay.snowmanOverlayTexture);
                        ALOAD_0,
                        reference(GETSTATIC, snowmanOverlayTexture),
                        reference(INVOKEVIRTUAL, loadTexture),

                        // MobOverlay.renderSnowmanOverlay();
                        reference(INVOKESTATIC, renderSnowmanOverlay),

                        // } else {
                        GOTO, branch("B"),
                        label("A"),

                        // ...
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(renderEquippedItems));
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "setupMooshroom", "(LEntityLiving;Ljava/lang/String;)Ljava/lang/String;"))
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
                        // renderBlocks.renderBlockAsItem(Block.mushroomRed, 0, 1.0f);
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
                        // if (!MobOverlay.renderMooshroomOverlay()) {
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderMooshroomOverlay", "()Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "finishMooshroom", "()V"))
                    );
                }
            }.targetMethod(renderEquippedItems));
        }
    }

    private class MiscSkinMod extends ClassMod {
        private final String className;

        MiscSkinMod(String className, final String texture) {
            final MethodRef randomTexture = new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;");

            this.className = className;
            global = true;

            classSignatures.add(new ConstSignature(texture));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "randomize " + texture;
                }

                @Override
                public String getMatchExpression() {
                    if ((getMethodInfo().getAccessFlags() & AccessFlag.STATIC) == 0 &&
                        getMethodInfo().getDescriptor().startsWith("(L")) {
                        return buildExpression(
                            push(texture)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_1,
                        getMatch(),
                        reference(INVOKESTATIC, randomTexture)
                    );
                }
            });
        }

        @Override
        public String getDeobfClass() {
            return className;
        }
    }
}
