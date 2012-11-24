package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterSkies extends Mod {
    private final boolean haveNewWorld;
    private final boolean haveNewWorldTime;
    private final String worldObjClass;

    public BetterSkies(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_SKIES;
        author = "MCPatcher";
        description = "Adds support for custom skyboxes.";
        version = "1.1";

        addDependency(BaseTexturePackMod.NAME);

        haveNewWorld = minecraftVersion.compareTo("12w18a") >= 0;
        haveNewWorldTime = minecraftVersion.compareTo("12w32a") >= 0;

        addClassMod(new BaseMod.MinecraftMod().addWorldGetter(minecraftVersion));
        addClassMod(new WorldMod());
        if (haveNewWorld) {
            addClassMod(new BaseMod.WorldServerMPMod(minecraftVersion));
            addClassMod(new BaseMod.WorldServerMod(minecraftVersion));
            worldObjClass = "WorldServerMP";
        } else {
            worldObjClass = "World";
        }
        addClassMod(new RenderGlobalMod());

        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS);
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$1");
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$WorldEntry");
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$Layer");
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            final MethodRef getWorldTime = new MethodRef(getDeobfClass(), "getWorldTime", "()J");

            if (haveNewWorldTime) {
                addMemberMapper(new MethodMapper(null, null, getWorldTime));
            } else {
                addMemberMapper(new MethodMapper(null, getWorldTime));
            }
        }
    }

    private class RenderGlobalMod extends ClassMod {
        private final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");

        RenderGlobalMod() {
            final MethodRef getTexture = new MethodRef("RenderEngine", "getTexture", "(Ljava/lang/String;)I");
            final MethodRef bindTexture = new MethodRef("RenderEngine", "bindTexture", "(I)V");
            final MethodRef getCelestialAngle = new MethodRef(worldObjClass, "getCelestialAngle", "(F)F");
            final MethodRef getRainStrength = new MethodRef("World", "getRainStrength", "(F)F");
            final MethodRef startDrawingQuads = new MethodRef("Tessellator", "startDrawingQuads", "()V");
            final MethodRef setColorOpaque_I = new MethodRef("Tessellator", "setColorOpaque_I", "(I)V");
            final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
            final MethodRef draw = new MethodRef("Tessellator", "draw", "()I");
            final MethodRef glRotatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glRotatef", "(FFFF)V");
            final MethodRef glCallList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glCallList", "(I)V");
            final FieldRef tessellator = new FieldRef("Tessellator", "instance", "LTessellator;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef worldProvider = new FieldRef("World", "worldProvider", "LWorldProvider;");
            final FieldRef worldType = new FieldRef("WorldProvider", "worldType", "I");
            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "L" + worldObjClass + ";");
            final FieldRef glSkyList = new FieldRef(getDeobfClass(), "glSkyList", "I");
            final FieldRef glSkyList2 = new FieldRef(getDeobfClass(), "glSkyList2", "I");
            final FieldRef glStarList = new FieldRef(getDeobfClass(), "glStarList", "I");
            final FieldRef active = new FieldRef(MCPatcherUtils.SKY_RENDERER_CLASS, "active", "Z");

            addClassSignature(new ConstSignature("smoke"));
            addClassSignature(new ConstSignature("/environment/clouds.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // mc.theWorld.worldProvider.worldType == 1
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(3),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        ICONST_1,

                        // ...
                        any(0, 100),

                        // renderEngine.bindTexture(renderEngine.getTexture("/misc/tunnel.png"));
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        backReference(4),
                        push("/misc/tunnel.png"),
                        captureReference(INVOKEVIRTUAL),
                        captureReference(INVOKEVIRTUAL),

                        // Tessellator tessellator = Tessellator.instance;
                        captureReference(GETSTATIC),
                        anyASTORE,

                        // ...
                        any(0, 1000),

                        // d = 1.0F - worldObj.getRainStrength(par1);
                        push(1.0f),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        FLOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        FSUB,
                        or(
                            build(
                                anyFSTORE
                            ),
                            build(
                                F2D,
                                anyDSTORE
                            )
                        ),

                        // ..
                        any(0, 500),

                        // GL11.glRotatef(worldObj.getCelestialAngle(par1) * 360F, 1.0F, 0.0F, 0.0F);
                        ALOAD_0,
                        backReference(8),
                        FLOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        push(360.0f),
                        FMUL,
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, glRotatef)
                    );
                }
            }
                .setMethod(renderSky)
                .addXref(1, mc)
                .addXref(2, worldProvider)
                .addXref(3, worldType)
                .addXref(4, renderEngine)
                .addXref(5, getTexture)
                .addXref(6, bindTexture)
                .addXref(7, tessellator)
                .addXref(8, worldObj)
                .addXref(9, getRainStrength)
                .addXref(10, getCelestialAngle)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glCallList),

                        nonGreedy(any(0, 1000)),

                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glCallList),

                        nonGreedy(any(0, 1000)),

                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glCallList)
                    );
                }
            }
                .setMethod(renderSky)
                .addXref(1, glSkyList)
                .addXref(2, glStarList)
                .addXref(3, glSkyList2)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.startDrawingQuads();
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.setColorOpaque_I(0x...);
                        ALOAD_2,
                        anyLDC,
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.addVertexWithUV(-100D, -100D, -100D, 0.0D, 0.0D);
                        ALOAD_2,
                        push(-100.0),
                        push(-100.0),
                        push(-100.0),
                        push(0.0),
                        push(0.0),
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.addVertexWithUV(-100D, -100D, 100D, 0.0D, 16D);
                        ALOAD_2,
                        push(-100.0),
                        push(-100.0),
                        push(100.0),
                        push(0.0),
                        push(16.0),
                        backReference(3),

                        // tessellator.addVertexWithUV(100D, -100D, 100D, 16D, 16D);
                        ALOAD_2,
                        push(100.0),
                        push(-100.0),
                        push(100.0),
                        push(16.0),
                        push(16.0),
                        backReference(3),

                        // tessellator.addVertexWithUV(100D, -100D, -100D, 16D, 0.0D);
                        ALOAD_2,
                        push(100.0),
                        push(-100.0),
                        push(-100.0),
                        push(16.0),
                        push(0.0),
                        backReference(3),

                        // tessellator.draw();
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),
                        POP
                    );
                }
            }
                .addXref(1, startDrawingQuads)
                .addXref(2, setColorOpaque_I)
                .addXref(3, addVertexWithUV)
                .addXref(4, draw)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "setup for sky rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        ALOAD_0,
                        reference(GETFIELD, renderEngine),
                        FLOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, getCelestialAngle),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setup", "(LWorld;LRenderEngine;FF)V"))
                    );
                }
            }.targetMethod(renderSky));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "render custom sky";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glRotatef(worldObj.getCelestialAngle(par1) * 360F, 1.0F, 0.0F, 0.0F);
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, getCelestialAngle),
                        push(360.0f),
                        FMUL,
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, glRotatef)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "renderAll", "()V"))
                    );
                }
            }.targetMethod(renderSky));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "disable default stars";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, capture(any()),
                        FLOAD, backReference(1),
                        FLOAD, backReference(1),
                        FLOAD, backReference(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V")),

                        ALOAD_0,
                        reference(GETFIELD, glStarList),
                        reference(INVOKESTATIC, glCallList)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, active),
                        IFNE, branch("A"),
                        getMatch(),
                        label("A")
                    );
                }
            }.targetMethod(renderSky));

            addCelestialObjectPatch("sun", "/terrain/sun.png");
            addCelestialObjectPatch("moon", "/terrain/moon_phases.png");
        }

        private void addCelestialObjectPatch(final String objName, final String textureName) {
            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override " + objName + " texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(textureName)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setupCelestialObject", "(Ljava/lang/String;)Ljava/lang/String;"))
                    );
                }
            }.targetMethod(renderSky));
        }
    }
}
