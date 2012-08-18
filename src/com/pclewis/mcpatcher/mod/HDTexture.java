package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class HDTexture extends BaseTexturePackMod {
    private final boolean haveColorizerWater;
    private final boolean haveGetImageRGB;

    public HDTexture(MinecraftVersion minecraftVersion) {
        super(minecraftVersion);
        clearPatches();

        name = MCPatcherUtils.HD_TEXTURES;
        author = "MCPatcher";
        description = "Provides support for high-resolution texture packs and custom animations.";
        version = "1.4";
        configPanel = new HDTextureConfig();

        addDependency(BaseTexturePackMod.NAME);

        haveColorizerWater = minecraftVersion.compareTo("Beta 1.6") >= 0;
        haveGetImageRGB = minecraftVersion.compareTo("Beta 1.6") >= 0;

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
        classMods.add(new ColorizerMod("ColorizerWater", haveColorizerWater ? "/misc/watercolor.png" : "/misc/foliagecolor.png"));
        classMods.add(new ColorizerMod("ColorizerGrass", "/misc/grasscolor.png"));
        classMods.add(new ColorizerMod("ColorizerFoliage", "/misc/foliagecolor.png"));

        if (minecraftVersion.compareTo("12w22a") < 0) {
            classMods.add(new GuiContainerCreativeMod());
        }

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_SIZE_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TEXTURE_UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TEXTURE_UTILS_CLASS + "$1"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TEXTURE_UTILS_CLASS + "$2"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Delegate"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Tile"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Strip"));
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            final MethodRef updateDynamicTextures = new MethodRef(getDeobfClass(), "updateDynamicTextures", "()V");
            final MethodRef readTextureImage = new MethodRef(getDeobfClass(), "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
            final MethodRef setupTexture = new MethodRef(getDeobfClass(), "setupTexture", "(Ljava/awt/image/BufferedImage;I)V");
            final MethodRef registerTextureFX = new MethodRef(getDeobfClass(), "registerTextureFX", "(LTextureFX;)V");
            final MethodRef refreshTextures = new MethodRef(getDeobfClass(), "refreshTextures", "()V");
            final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
            final FieldRef imageData = new FieldRef(getDeobfClass(), "imageData", "Ljava/nio/ByteBuffer;");
            final FieldRef textureList = new FieldRef(getDeobfClass(), "textureList", "Ljava/util/List;");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)I");
            final MethodRef getImageRGB = new MethodRef(getDeobfClass(), "getImageRGB", "(Ljava/awt/image/BufferedImage;[I)[I");
            final MethodRef readTextureImageData = new MethodRef(getDeobfClass(), "readTextureImageData", "(Ljava/lang/String;)[I");

            final int getInputStreamOpcode;
            final JavaRef getInputStream;
            if (haveITexturePack) {
                getInputStreamOpcode = INVOKEINTERFACE;
                getInputStream = new InterfaceMethodRef("ITexturePack", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            } else {
                getInputStreamOpcode = INVOKEVIRTUAL;
                getInputStream = new MethodRef("TexturePackBase", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            }

            classSignatures.add(new ConstSignature(glTexSubImage2D));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("%clamp%")
                    );
                }
            }.setMethod(refreshTextures));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, glTexSubImage2D)
                    );
                }
            }.setMethod(updateDynamicTextures));

            memberMappers.add(new FieldMapper(imageData));
            memberMappers.add(new FieldMapper(textureList));
            memberMappers.add(new MethodMapper(registerTextureFX));
            memberMappers.add(new MethodMapper(readTextureImage));
            memberMappers.add(new MethodMapper(setupTexture));
            memberMappers.add(new MethodMapper(getTexture));
            if (haveGetImageRGB) {
                memberMappers.add(new MethodMapper(getImageRGB));
            }
            if (haveColorizerWater) {
                memberMappers.add(new MethodMapper(readTextureImageData));
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
                        push(0x1908), // GL_RGBA
                        push(0x1401) // GL_UNSIGNED_BYTE
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                        push(0x1908), // GL_RGBA
                        push(0x1401) // GL_UNSIGNED_BYTE
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "readTextureImage(getInputStream(...)) -> getImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(getInputStreamOpcode, getInputStream),
                        reference(INVOKESPECIAL, readTextureImage)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "getImage", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "getInputStream(...), readTextureImage -> getImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        ALOAD_1,
                        reference(getInputStreamOpcode, getInputStream),
                        BytecodeMatcher.anyASTORE,
                        BytecodeMatcher.anyALOAD,
                        IFNONNULL, BinaryRegex.any(2),
                        ALOAD_0,
                        ALOAD_0,
                        GETFIELD, BinaryRegex.any(2),
                        BytecodeMatcher.anyILOAD,
                        reference(INVOKEVIRTUAL, setupTexture),
                        GOTO, BinaryRegex.any(2),
                        ALOAD_0,
                        ALOAD_0,
                        BytecodeMatcher.anyALOAD,
                        reference(INVOKESPECIAL, readTextureImage)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "imageData.clear(), .put(), .limit() -> imageData = TextureUtils.getByteBuffer()";
                }

                @Override
                public String getMatchExpression() {
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
                        reference(GETFIELD, textureList),
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "registerTextureFX", "(Ljava/util/List;LTextureFX;)V")),
                        RETURN
                    );
                }
            }.targetMethod(registerTextureFX));

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
            }.targetMethod(setupTexture));

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
            }.targetMethod(getImageRGB));

            patches.add(new TileSizePatch(1048576, "int_glBufferSize"));

            patches.add(new AddMethodPatch(new MethodRef(getDeobfClass(), "reloadTextures", "(Lnet/minecraft/client/Minecraft;)V")) {
                @Override
                public byte[] generateMethod() throws IOException {
                    return buildCode(
                        // imageData = GLAllocation.createDirectByteBuffer(TileSize.int_glBufferSize);
                        ALOAD_0,
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_glBufferSize", "I")),
                        reference(INVOKESTATIC, new MethodRef("GLAllocation", "createDirectByteBuffer", "(I)Ljava/nio/ByteBuffer;")),
                        reference(PUTFIELD, imageData),

                        // refreshTextures();
                        ALOAD_0,
                        reference(INVOKEVIRTUAL, new MethodRef(getDeobfClass(), "refreshTextures", "()V")),

                        // TextureUtils.refreshTextureFX(textureList);
                        ALOAD_0,
                        reference(GETFIELD, textureList),
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
            final FieldRef imageData = new FieldRef(getDeobfClass(), "imageData", "[B");
            final FieldRef tileNumber = new FieldRef(getDeobfClass(), "tileNumber", "I");
            final FieldRef tileSize = new FieldRef(getDeobfClass(), "tileSize", "I");
            final FieldRef tileImage = new FieldRef(getDeobfClass(), "tileImage", "I");
            final MethodRef bindImage = new MethodRef(getDeobfClass(), "bindImage", "(LRenderEngine;)V");

            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0x04, 0x00, // 1024
                NEWARRAY, T_BYTE
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                RETURN,
                BinaryRegex.end()
            ).setMethodName("onTick"));

            memberMappers.add(new FieldMapper(imageData));
            memberMappers.add(new FieldMapper(tileNumber, null, tileSize, tileImage));

            memberMappers.add(new MethodMapper(bindImage));

            patches.add(new TileSizePatch.ArraySizePatch(1024, "int_numBytes"));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "check for bindImage recursion (end)";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "bindImageEnd", "()V"))
                    );
                }
            }.targetMethod(bindImage));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for bindImage recursion (start)";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "bindImageBegin", "()Z")),
                        IFNE, branch("A"),

                        RETURN,

                        label("A")
                    );
                }
            }.targetMethod(bindImage));
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
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");

            if (haveColorizerWater) {
                addColorizerSignature("Water");
                addColorizerSignature("Grass");
                addColorizerSignature("Foliage");
            }

            memberMappers.add(new FieldMapper(renderEngine));
        }

        private void addColorizerSignature(final String name) {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/misc/" + name.toLowerCase() + "color.png"),
                        BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                        BytecodeMatcher.captureReference(INVOKESTATIC)
                    );
                }
            }.addXref(1, new MethodRef("Colorizer" + name, "loadColorBuffer", "([I)V")));
        }
    }

    private class ColorizerMod extends ClassMod {
        private final String name;

        ColorizerMod(String name, String resource) {
            this.name = name;

            final FieldRef colorBuffer = new FieldRef(getDeobfClass(), "colorBuffer", "[I");

            memberMappers.add(new FieldMapper(colorBuffer));

            patches.add(new MakeMemberPublicPatch(colorBuffer));

            if (haveColorizerWater) {
                prerequisiteClasses.add("Minecraft");
            } else {
                classSignatures.add(new ConstSignature(resource));
            }
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }

    private class GuiContainerCreativeMod extends ClassMod {
        GuiContainerCreativeMod() {
            global = true;

            classSignatures.add(new ConstSignature("/gui/allitems.png"));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "use allitemsx.png for creative mode inventory background";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/gui/allitems.png")
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        push(true),
                        reference(PUTSTATIC, new FieldRef(MCPatcherUtils.TEXTURE_UTILS_CLASS, "oldCreativeGui", "Z"))
                    );
                }
            });
        }
    }
}
