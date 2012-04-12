package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.*;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class HDTexture extends Mod {
    private final boolean haveColorizerWater;
    private final boolean haveAlternateFont;
    private final boolean haveUnicode;
    private final boolean haveGetImageRGB;
    private final boolean haveFolderTexturePacks;
    private final boolean haveITexturePack;

    public HDTexture(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.HD_TEXTURES;
        author = "MCPatcher";
        description = "Provides support for texture packs of size 32x32 and higher.";
        version = "1.3";
        configPanel = new HDTextureConfig();

        haveColorizerWater = minecraftVersion.compareTo("Beta 1.6") >= 0;
        haveAlternateFont = minecraftVersion.compareTo("Beta 1.9 Prerelease 3") >= 0;
        haveUnicode = minecraftVersion.compareTo("11w49a") >= 0 || minecraftVersion.compareTo("1.0.1") >= 0;
        haveGetImageRGB = minecraftVersion.compareTo("Beta 1.6") >= 0;
        haveFolderTexturePacks = minecraftVersion.compareTo("12w08a") >= 0;
        haveITexturePack = minecraftVersion.compareTo("12w15a") >= 0;

        classMods.add(new RenderEngineMod());
        classMods.add(new TextureFXMod());
        classMods.add(new CompassMod());
        classMods.add(new FireMod());
        classMods.add(new FluidMod("StillLava"));
        classMods.add(new FluidMod("FlowLava"));
        classMods.add(new FluidMod("StillWater"));
        classMods.add(new FluidMod("FlowWater"));
        classMods.add(new ItemRendererMod());
        classMods.add(new WatchMod());
        classMods.add(new PortalMod());
        classMods.add(new MinecraftMod());
        classMods.add(new BaseMod.GLAllocationMod());
        classMods.add(new TexturePackListMod(minecraftVersion));
        classMods.add(new TexturePackBaseMod(minecraftVersion));
        classMods.add(new TexturePackCustomMod());
        classMods.add(new BaseMod.TexturePackDefaultMod());
        if (haveFolderTexturePacks) {
            classMods.add(new TexturePackFolderMod());
        }
        classMods.add(new FontRendererMod());
        classMods.add(new GameSettingsMod());
        classMods.add(new GetResourceMod());
        if (haveColorizerWater) {
            classMods.add(new ColorizerMod("ColorizerWater", false, false));
            classMods.add(new ColorizerMod("ColorizerGrass", true, false));
            classMods.add(new ColorizerMod("ColorizerFoliage", true, true));
        } else {
            classMods.add(new ColorizerMod("ColorizerWater", "/misc/foliagecolor.png"));
            classMods.add(new ColorizerMod("ColorizerGrass", "/misc/grasscolor.png"));
            classMods.add(new ColorizerMod("ColorizerFoliage", "/misc/foliagecolor.png"));
        }

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_SIZE_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TEXTURE_UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TEXTURE_UTILS_CLASS + "$1"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Delegate"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Tile"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Strip"));
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            final MethodRef updateDynamicTextures = new MethodRef(getDeobfClass(), "updateDynamicTextures", "()V");

            classSignatures.add(new ConstSignature(new MethodRef("org.lwjgl.opengl.GL11", "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals("()V")) {
                        return buildExpression(
                            push("%clamp%")
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethodName("refreshTextures"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V"))
                    );
                }
            }.setMethod(updateDynamicTextures));

            memberMappers.add(new FieldMapper("imageData", "Ljava/nio/ByteBuffer;"));
            memberMappers.add(new FieldMapper("textureList", "Ljava/util/List;"));
            memberMappers.add(new MethodMapper("registerTextureFX", "(LTextureFX;)V"));
            memberMappers.add(new MethodMapper("readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"));
            memberMappers.add(new MethodMapper("setupTexture", "(Ljava/awt/image/BufferedImage;I)V"));
            memberMappers.add(new MethodMapper("getTexture", "(Ljava/lang/String;)I"));
            if (haveGetImageRGB) {
                memberMappers.add(new MethodMapper("getImageRGB", "(Ljava/awt/image/BufferedImage;[I)[I"));
            }
            if (haveColorizerWater) {
                memberMappers.add(new MethodMapper("readTextureImageData", "(Ljava/lang/String;)[I"));
            }

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    String op = (getCaptureGroup(1)[0] == IREM ? "%" : "/");
                    return String.format("(i %1$s 16) * 16 + j * 16 -> (i %1$s 16) * int_size + j * int_size", op);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(16),
                        BinaryRegex.capture(BinaryRegex.subset(new byte[]{IREM, IDIV}, true)),
                        push(16),
                        IMUL,
                        BinaryRegex.capture(BinaryRegex.any(1, 3)),
                        push(16),
                        IMUL
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        push(16),
                        getCaptureGroup(1),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                        IMUL,
                        getCaptureGroup(2),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                        IMUL
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "glTexSubImage2D(...,16,16) -> glTexSubImage2D(...,int_size,int_size)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(16),
                        push(16),
                        SIPUSH, 0x19, 0x08, // GL_RGBA
                        SIPUSH, 0x14, 0x01  // GL_UNSIGNED_BYTE
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                        SIPUSH, 0x19, 0x08,
                        SIPUSH, 0x14, 0x01
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "readTextureImage(getInputStream(...)) -> getResourceAsBufferedImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, new MethodRef("TexturePackBase", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;")),
                        reference(INVOKESPECIAL, new MethodRef("RenderEngine", "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "getResourceAsBufferedImage", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "getInputStream(...), readTextureImage -> getResourceAsBufferedImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, new MethodRef("TexturePackBase", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;")),
                        BytecodeMatcher.anyASTORE,
                        BytecodeMatcher.anyALOAD,
                        IFNONNULL, BinaryRegex.any(2),
                        ALOAD_0,
                        ALOAD_0,
                        GETFIELD, BinaryRegex.any(2),
                        BytecodeMatcher.anyILOAD,
                        reference(INVOKEVIRTUAL, new MethodRef("RenderEngine", "setupTexture", "(Ljava/awt/image/BufferedImage;I)V")),
                        GOTO, BinaryRegex.any(2),
                        ALOAD_0,
                        ALOAD_0,
                        BytecodeMatcher.anyALOAD,
                        reference(INVOKESPECIAL, new MethodRef("RenderEngine", "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "getResourceAsBufferedImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                private FieldRef imageData;

                @Override
                public String getDescription() {
                    return "imageData.clear(), .put(), .limit() -> imageData = TextureUtils.getByteBuffer()";
                }

                @Override
                public String getMatchExpression() {
                    imageData = new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;");
                    return buildExpression(
                        // imageData.clear();
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP,

                        // imageData.put($1);
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        BinaryRegex.capture(BinaryRegex.any(2, 5)),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "put", "([B)Ljava/nio/ByteBuffer;")),
                        POP,

                        // imageData.position(0).limit($1.length);
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        ICONST_0,
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "position", "(I)Ljava/nio/Buffer;")),
                        BinaryRegex.backReference(1),
                        ARRAYLENGTH,
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/Buffer", "limit", "(I)Ljava/nio/Buffer;")),
                        POP
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // imageData = TextureUtils.getByteBuffer(imageData, $1);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "getByteBuffer", "(Ljava/nio/ByteBuffer;[B)Ljava/nio/ByteBuffer;")),
                        reference(PUTFIELD, imageData)
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call TextureUtils.registerTextureFX";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        BinaryRegex.any(0, 50),
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef("RenderEngine", "textureList", "Ljava/util/List;")),
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "registerTextureFX", "(Ljava/util/List;LTextureFX;)V")),
                        RETURN
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "registerTextureFX", "(LTextureFX;)V")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in setupTexture";
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
                        ALOAD_1,
                        IFNONNULL, branch("A"),
                        RETURN,
                        label("A")
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "setupTexture", "(Ljava/awt/image/BufferedImage;I)V")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in getImageRGB";
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
                        ALOAD_1,
                        IFNONNULL, branch("A"),
                        ALOAD_2,
                        ARETURN,
                        label("A")
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "getImageRGB", "(Ljava/awt/image/BufferedImage;[I)[I")));

            patches.add(new TileSizePatch(1048576, "int_glBufferSize"));

            patches.add(new AddMethodPatch("setTileSize", "(Lnet/minecraft/client/Minecraft;)V") {
                @Override
                public byte[] generateMethod() throws IOException {
                    maxStackSize = 10;
                    numLocals = 5;
                    return buildCode(
                        // imageData = GLAllocation.createDirectByteBuffer(TileSize.int_glBufferSize);
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_glBufferSize", "I")),
                        reference(INVOKESTATIC, new MethodRef("GLAllocation", "createDirectByteBuffer", "(I)Ljava/nio/ByteBuffer;")),
                        reference(PUTFIELD, new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;")),

                        // refreshTextures();
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, new MethodRef("RenderEngine", "refreshTextures", "()V")),

                        // TextureUtils.refreshTextureFX(textureList);
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef("RenderEngine", "textureList", "Ljava/util/List;")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "refreshTextureFX", "(Ljava/util/List;)V")),

                        RETURN
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "update custom animations";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CUSTOM_ANIMATION_CLASS, "updateAll", "()V"))
                    );
                }
            }.targetMethod(updateDynamicTextures));
        }
    }

    private class TextureFXMod extends ClassMod {
        TextureFXMod() {
            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0x04, 0x00, // 1024
                NEWARRAY, T_BYTE
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                RETURN,
                BinaryRegex.end()
            ).setMethodName("onTick"));

            memberMappers.add(new FieldMapper("imageData", "[B"));
            memberMappers.add(new FieldMapper(new String[]{"tileNumber", null, "tileSize", "tileImage"}, "I"));

            patches.add(new TileSizePatch.ArraySizePatch(1024, "int_numBytes"));
        }
    }

    private class CompassMod extends ClassMod {
        CompassMod() {
            classSignatures.add(new ConstSignature("/gui/items.png"));
            classSignatures.add(new ConstSignature("/misc/dial.png").negate(true));
            classSignatures.add(new ConstSignature(new MethodRef("java.lang.Math", "sin", "(D)D")));

            classSignatures.add(new FixedBytecodeSignature(
                ALOAD_0,
                SIPUSH, 0x01, 0x00, // 256
                NEWARRAY, T_INT,
                PUTFIELD, BinaryRegex.any(2),
                ALOAD_0
            ));

            patches.add(new TileSizePatch(7.5, "double_compassCenterMin"));
            patches.add(new TileSizePatch(8.5, "double_compassCenterMax"));
            patches.add(new TileSizePatch.ArraySizePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.MultiplyPatch(16, "int_size"));
            patches.add(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch(-4, "int_compassCrossMin"));
            patches.add(new TileSizePatch.IfGreaterPatch(4, "int_compassCrossMax"));
            patches.add(new TileSizePatch(-8, "int_compassNeedleMin"));
            patches.add(new TileSizePatch.IfGreaterPatch(16, "int_compassNeedleMax"));
            patches.add(new TileSizePatch.GetRGBPatch());
        }
    }

    private class FireMod extends ClassMod {
        FireMod() {
            classSignatures.add(new ConstSignature(new MethodRef("java.lang.Math", "random", "()D")));

            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0x01, 0x40, // 320
                NEWARRAY, T_FLOAT,
                PUTFIELD, BinaryRegex.any(2),
                ALOAD_0,
                SIPUSH, 0x01, 0x40, // 320
                NEWARRAY, T_FLOAT,
                PUTFIELD, BinaryRegex.any(2),
                RETURN
            ));

            patches.add(new TileSizePatch(1.06f, "float_flameNudge"));
            patches.add(new TileSizePatch(1.0600001f, "float_flameNudge"));
            patches.add(new TileSizePatch.ArraySizePatch(320, "int_flameArraySize"));
            patches.add(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.WhilePatch(20, "int_flameHeight"));
            patches.add(new TileSizePatch.WhilePatch(16, "int_size"));
            patches.add(new TileSizePatch.ModPatch(20, "int_flameHeight"));
            patches.add(new TileSizePatch.IfLessPatch(19, "int_flameHeightMinus1"));

            patches.add(new TileSizePatch.MultiplyPatch(16, "int_size") {
                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    return !getMethodInfo().isConstructor();
                }
            });
        }
    }

    private class FluidMod extends ClassMod {
        private String name;

        FluidMod(String name) {
            this.name = name;
            boolean lava = name.contains("Lava");
            boolean flow = name.contains("Flow");

            classSignatures.add(new FixedBytecodeSignature(
                ALOAD_0,
                GETSTATIC, BinaryRegex.any(2),
                GETFIELD, BinaryRegex.any(2),
                (flow ? new byte[]{ICONST_1, IADD} : new byte[0]),
                INVOKESPECIAL
            ));

            final double rand = (lava ? 0.005 : flow ? 0.2 : 0.05);

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("java.lang.Math", "random", "()D")),
                        push(rand),
                        DCMPG,
                        IFGE
                    );
                }
            });

            if (lava) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            I2F,
                            push(3.1415927F),
                            FMUL,
                            FCONST_2,
                            FMUL,
                            push(16.0F),
                            FDIV
                        );
                    }
                });
            }

            patches.add(new TileSizePatch.ArraySizePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.WhilePatch(16, "int_size"));
            patches.add(new TileSizePatch.BitMaskPatch(255, "int_numPixelsMinus1"));
            patches.add(new TileSizePatch.BitMaskPatch(15, "int_sizeMinus1"));
            patches.add(new TileSizePatch.MultiplyPatch(16, "int_size"));
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }

    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            classSignatures.add(new ConstSignature(-0.9375F));
            classSignatures.add(new ConstSignature(0.0625F));
            classSignatures.add(new ConstSignature(0.001953125F));

            patches.add(new TileSizePatch.ToolPixelTopPatch());
            patches.add(new TileSizePatch(16.0F, "float_size"));
            patches.add(new TileSizePatch.WhilePatch(16, "int_size"));
            patches.add(new TileSizePatch.ToolTexPatch());
            patches.add(new TileSizePatch(0.001953125F, "float_texNudge"));
        }
    }

    private class WatchMod extends ClassMod {
        public WatchMod() {
            classSignatures.add(new ConstSignature("/misc/dial.png"));

            patches.add(new TileSizePatch(16.0D, "double_size"));
            patches.add(new TileSizePatch(15.0D, "double_sizeMinus1"));
            patches.add(new TileSizePatch.GetRGBPatch());
            patches.add(new TileSizePatch.ArraySizePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.MultiplyPatch(16, "int_size"));
            patches.add(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.BitMaskPatch(15, "int_sizeMinus1"));
            patches.add(new TileSizePatch.DivPatch(16, "int_size"));

            patches.add(new TileSizePatch.ModPatch(16, "int_size") {
                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    return !getMethodInfo().isConstructor();
                }
            });
        }
    }

    private class PortalMod extends ClassMod {
        PortalMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, BinaryRegex.any(),
                        F2D,
                        FLOAD, BinaryRegex.any(),
                        F2D,
                        reference(INVOKESTATIC, new MethodRef("java.lang.Math", "atan2", "(DD)D")),
                        D2F
                    );
                }
            });

            patches.add(new TileSizePatch(16.0F, "float_size"));
            patches.add(new TileSizePatch.WhilePatch(16, "int_size"));
            patches.add(new TileSizePatch.ArraySize2DPatch(1024, "int_numBytes", 32));
            patches.add(new TileSizePatch.MultiplyPatch(16, "int_size"));
            patches.add(new TileSizePatch.MultiplyPatch(8, "int_sizeHalf"));
            patches.add(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new TileSizePatch.IfLessPatch(256, "int_numPixels"));
        }
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            mapTexturePackList();

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/terrain.png")
                    );
                }
            }.setMethodName("runTick"));

            memberMappers.add(new FieldMapper("renderEngine", "LRenderEngine;"));
            memberMappers.add(new FieldMapper("gameSettings", "LGameSettings;"));
            if (haveAlternateFont) {
                memberMappers.add(new FieldMapper(new String[]{"fontRenderer", "alternateFontRenderer"}, "LFontRenderer;"));
            } else {
                memberMappers.add(new FieldMapper("fontRenderer", "LFontRenderer;"));
                memberMappers.add(new FieldMapper("alternateFontRenderer", "LFontRenderer;"));
            }

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "TextureUtils.setTileSize(), renderEngine.setTileSize() on startup";
                }

                @Override
                public String getMatchExpression() {
                    return BinaryRegex.capture(buildExpression(
                        ALOAD_0,
                        reference(NEW, new ClassRef("RenderEngine")),
                        BinaryRegex.any(0, 18),
                        PUTFIELD, BinaryRegex.capture(BinaryRegex.any(2))
                    ));
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "setTileSize", "()Z")),
                        POP,
                        ALOAD_0,
                        GETFIELD, getCaptureGroup(2),
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, new MethodRef("RenderEngine", "setTileSize", "(LMinecraft;)V"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                private JavaRef renderEngine = new FieldRef("Minecraft", "renderEngine", "LRenderEngine;");
                private JavaRef registerTextureFX = new MethodRef("RenderEngine", "registerTextureFX", "(LTextureFX;)V");

                @Override
                public String getDescription() {
                    return "remove registerTextureFX call";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, renderEngine),
                        BinaryRegex.any(0, 10),
                        reference(INVOKEVIRTUAL, registerTextureFX)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return new byte[0];
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for texture pack change on each tick";
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
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "checkTexturePackChange", "(LMinecraft;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "runTick", "()V")));
        }
    }

    private class TexturePackListMod extends BaseMod.TexturePackListMod {
        TexturePackListMod(MinecraftVersion minecraftVersion) {
            super(minecraftVersion);
            memberMappers.add(new MethodMapper("updateAvailableTexturePacks", "()V"));
            if (useITexturePack) {
                memberMappers.add(new MethodMapper("setTexturePack", "(LITexturePack;)Z"));
            } else {
                memberMappers.add(new MethodMapper("setTexturePack", "(LTexturePackBase;)Z"));
            }
            memberMappers.add(new MethodMapper("availableTexturePacks", "()Ljava/util/List;"));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "defaultTexturePack", "LTexturePackBase;")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "TexturePackList.setTileSize(selectedTexturePack) on texture pack change";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(buildExpression(
                            ALOAD_0,
                            reference(GETFIELD, new FieldRef("TexturePackList", "selectedTexturePack", "LTexturePackBase;")),
                            INVOKEVIRTUAL, BinaryRegex.any(2)
                        )),
                        BinaryRegex.capture(buildExpression(
                            ICONST_1,
                            IRETURN
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "setTileSize", "()Z")),
                        POP,
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef("TexturePackList", "minecraft", "LMinecraft;")),
                        DUP,
                        reference(GETFIELD, new FieldRef("Minecraft", "renderEngine", "LRenderEngine;")),
                        SWAP,
                        reference(INVOKEVIRTUAL, new MethodRef("RenderEngine", "setTileSize", "(LMinecraft;)V")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "setFontRenderer", "()V")),
                        getCaptureGroup(2)
                    );
                }
            });

            if (useITexturePack) {
                patches.add(new AddMethodPatch("setTexturePack", "(LTexturePackBase;)Z") {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            ALOAD_1,
                            reference(INVOKEVIRTUAL, new MethodRef(getDeobfClass(), "setTexturePack", "(LITexturePack;)Z")),
                            IRETURN
                        );
                    }
                });
            }
        }
    }

    private class TexturePackBaseMod extends BaseMod.TexturePackBaseMod {
        TexturePackBaseMod(MinecraftVersion minecraftVersion) {
            super(minecraftVersion);

            final String[] names = {"openTexturePackFile", "closeTexturePackFile"};
            if (useITexturePack) {
                memberMappers.add(new MethodMapper(new String[]{names[0] + "1", names[1] + "1"}, "(LRenderEngine;)V"));

                for (final String n : names) {
                    patches.add(new AddMethodPatch(n, "()V") {
                        @Override
                        public byte[] generateMethod() throws BadBytecode, IOException {
                            return buildCode(
                                ALOAD_0,
                                reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "getMinecraft", "()LMinecraft;")),
                                reference(GETFIELD, new FieldRef("Minecraft", "renderEngine", "LRenderEngine;")),
                                reference(INVOKEVIRTUAL, new MethodRef(getDeobfClass(), n + "1", "(LRenderEngine;)V")),
                                RETURN
                            );
                        }
                    });
                }

                memberMappers.add(new FieldMapper("file", "Ljava/io/File;"));

                patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "file", "Ljava/io/File;")) {
                    @Override
                    public int getNewFlags(int oldFlags) {
                        return (oldFlags & ~(AccessFlag.PRIVATE | AccessFlag.PROTECTED | AccessFlag.FINAL)) | AccessFlag.PUBLIC;
                    }
                });
            } else {
                memberMappers.add(new MethodMapper(new String[]{null, names[0], names[1]}, "()V"));
            }
        }
    }

    private class TexturePackCustomMod extends ClassMod {
        TexturePackCustomMod() {
            parentClass = "TexturePackBase";

            classSignatures.add(new ConstSignature(new ClassRef("java.util.zip.ZipFile")));
            if (!haveITexturePack) {
                classSignatures.add(new ConstSignature("pack.txt"));
                classSignatures.add(new ConstSignature("pack.png"));

                memberMappers.add(new FieldMapper("file", "Ljava/io/File;"));

                patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "file", "Ljava/io/File;")));
            }

            memberMappers.add(new FieldMapper("zipFile", "Ljava/util/zip/ZipFile;"));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "zipFile", "Ljava/util/zip/ZipFile;")));

            patches.add(new AddFieldPatch("origZip", "Ljava/util/zip/ZipFile;"));
            patches.add(new AddFieldPatch("tmpFile", "Ljava/io/File;"));
            patches.add(new AddFieldPatch("lastModified", "J"));

            String methodDescriptor = haveITexturePack ? "(LRenderEngine;)V" : "()V";

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "openTexturePackFile(this)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "openTexturePackFile", "(LTexturePackCustom;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "openTexturePackFile", methodDescriptor)));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "closeTexturePackFile(this)";
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
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "closeTexturePackFile", "(LTexturePackCustom;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "closeTexturePackFile", methodDescriptor)));
        }
    }

    private class TexturePackFolderMod extends ClassMod {
        TexturePackFolderMod() {
            parentClass = "TexturePackBase";

            final String fileField;

            if (haveITexturePack) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            push(1),
                            reference(INVOKEVIRTUAL, new MethodRef("java/lang/String", "substring", "(I)Ljava/lang/String;"))
                        );
                    }
                });

                fileField = "file";
            } else {
                classSignatures.add(new ConstSignature("pack.txt"));
                classSignatures.add(new ConstSignature("pack.png"));

                fileField = "folder";
                memberMappers.add(new FieldMapper(fileField, "Ljava/io/File;"));
            }
            classSignatures.add(new ConstSignature(new ClassRef("java.io.FileInputStream")));

            patches.add(new AddMethodPatch("getFolder", "()Ljava/io/File;") {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), fileField, "Ljava/io/File;")),
                        ARETURN
                    );
                }
            });
        }
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        FontRendererMod() {
            if (haveUnicode) {
                FieldRef isUnicode = new FieldRef(getDeobfClass(), "isUnicode", "Z");

                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        if (getMethodInfo().isConstructor()) {
                            return buildExpression(
                                ALOAD_0,
                                ILOAD, 4,
                                BytecodeMatcher.captureReference(PUTFIELD)
                            );
                        } else {
                            return null;
                        }
                    }
                }.addXref(1, isUnicode));

                patches.add(new MakeMemberPublicPatch(isUnicode));
            } else {
                patches.add(new AddFieldPatch("isUnicode", "Z"));
            }

            patches.add(new AddMethodPatch("initialize", "()V") {
                MethodInfo constructor;

                @Override
                public void prePatch(ClassFile classFile) {
                    constructor = null;
                }

                @Override
                public byte[] generateMethod() {
                    getDescriptor();
                    CodeAttribute ca = constructor.getCodeAttribute();
                    getMethodInfo().setDescriptor(constructor.getDescriptor().replace("Z)", ")"));
                    maxStackSize = ca.getMaxStack();
                    numLocals = ca.getMaxLocals();
                    exceptionTable = ca.getExceptionTable();
                    byte[] code = ca.getCode().clone();
                    if (haveUnicode) {  // remove java.lang.Object<init> call
                        code[0] = ICONST_0;
                        code[1] = ISTORE;
                        code[2] = 4;
                    } else {
                        code[0] = NOP;
                        code[1] = NOP;
                        code[2] = NOP;
                    }
                    code[3] = NOP;
                    return code;
                }

                @Override
                public String getDescriptor() {
                    if (constructor == null) {
                        for (Object o : getClassFile().getMethods()) {
                            MethodInfo method = (MethodInfo) o;
                            if (method.isConstructor() &&
                                ((haveUnicode && method.getDescriptor().contains("Z)")) ||
                                    (!haveUnicode && !method.getDescriptor().equals("()V")))) {
                                constructor = method;
                                break;
                            }
                        }
                        if (constructor == null) {
                            throw new RuntimeException("could not find FontRenderer constructor");
                        }
                    }
                    return constructor.getDescriptor().replace("Z)", ")");
                }
            });
        }
    }

    private class GameSettingsMod extends ClassMod {
        GameSettingsMod() {
            classSignatures.add(new ConstSignature("options.txt"));
            classSignatures.add(new OrSignature(
                new ConstSignature("key.forward"),
                new ConstSignature("Forward")
            ));
        }
    }

    private class GetResourceMod extends ClassMod {
        GetResourceMod() {
            global = true;

            final MethodRef getResource = new MethodRef("java.lang.Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;");
            final MethodRef readURL = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/net/URL;)Ljava/awt/image/BufferedImage;");
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            final MethodRef readStream = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

            classSignatures.add(new OrSignature(
                new ConstSignature(getResource),
                new ConstSignature(getResourceAsStream)
            ));
            classSignatures.add(new OrSignature(
                new ConstSignature(readURL),
                new ConstSignature(readStream)
            ));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "ImageIO.read(getResource(...)) -> getResourceAsBufferedImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.or(
                            buildExpression(
                                reference(INVOKEVIRTUAL, getResource),
                                reference(INVOKESTATIC, readURL)
                            ),
                            buildExpression(
                                reference(INVOKEVIRTUAL, getResourceAsStream),
                                reference(INVOKESTATIC, readStream)
                            )
                        )
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "getResourceAsBufferedImage", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });
        }
    }

    private class ColorizerMod extends ClassMod {
        private String name;

        private ColorizerMod(String name) {
            this.name = name;

            memberMappers.add(new FieldMapper("colorBuffer", "[I"));

            patches.add(new MakeMemberPublicPatch(new FieldRef(name, "colorBuffer", "[I")));
        }

        ColorizerMod(String name, String resource) {
            this(name);

            classSignatures.add(new ConstSignature(resource));
        }

        ColorizerMod(String name, boolean has255, boolean has6396257) {
            this(name);

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isStaticInitializer()) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            push(65536),
                            NEWARRAY, T_INT,
                            PUTSTATIC, BinaryRegex.any(2),
                            RETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }
            });

            classSignatures.add(new ConstSignature(6396257).negate(!has6396257));
            classSignatures.add(new ConstSignature(255.0).negate(!has255));
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }
}
