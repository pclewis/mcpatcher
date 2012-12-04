package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
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

        addClassMod(new RenderEngineMod());
        addClassMod(new TextureFXMod());
        addClassMod(new CompassMod());
        addClassMod(new FireMod());
        addClassMod(new FluidMod("StillLava"));
        addClassMod(new FluidMod("FlowLava"));
        addClassMod(new FluidMod("StillWater"));
        addClassMod(new FluidMod("FlowWater"));
        addClassMod(new ItemRendererMod());
        addClassMod(new WatchMod());
        addClassMod(new PortalMod());
        addClassMod(new MinecraftMod());
        addClassMod(new BaseMod.GLAllocationMod());
        addClassMod(new ColorizerMod("ColorizerWater", haveColorizerWater ? "/misc/watercolor.png" : "/misc/foliagecolor.png"));
        addClassMod(new ColorizerMod("ColorizerGrass", "/misc/grasscolor.png"));
        addClassMod(new ColorizerMod("ColorizerFoliage", "/misc/foliagecolor.png"));

        if (minecraftVersion.compareTo("12w22a") < 0) {
            addClassMod(new GuiContainerCreativeMod());
        }

        addClassFile(MCPatcherUtils.TILE_SIZE_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_UTILS_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_UTILS_CLASS + "$1");
        addClassFile(MCPatcherUtils.TEXTURE_UTILS_CLASS + "$2");
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS);
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Delegate");
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Tile");
        addClassFile(MCPatcherUtils.CUSTOM_ANIMATION_CLASS + "$Strip");
        addClassFile(MCPatcherUtils.FANCY_COMPASS_CLASS);
    }

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            final MethodRef updateDynamicTextures = new MethodRef(getDeobfClass(), "updateDynamicTextures", "()V");
            final MethodRef readTextureImage = new MethodRef(getDeobfClass(), "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");
            final MethodRef setupTexture = new MethodRef(getDeobfClass(), "setupTexture", "(Ljava/awt/image/BufferedImage;I)V");
            final MethodRef registerTextureFX = new MethodRef(getDeobfClass(), "registerTextureFX", "(LTextureFX;)V");
            final MethodRef refreshTextures = new MethodRef(getDeobfClass(), "refreshTextures", "()V");
            final FieldRef clampTexture = new FieldRef(getDeobfClass(), "clampTexture", "Z");
            final FieldRef blurTexture = new FieldRef(getDeobfClass(), "blurTexture", "Z");
            final FieldRef imageData = new FieldRef(getDeobfClass(), "imageData", "Ljava/nio/ByteBuffer;");
            final FieldRef textureList = new FieldRef(getDeobfClass(), "textureList", "Ljava/util/List;");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)I");
            final MethodRef getImageRGB = new MethodRef(getDeobfClass(), "getImageRGB", "(Ljava/awt/image/BufferedImage;[I)[I");
            final MethodRef readTextureImageData = new MethodRef(getDeobfClass(), "readTextureImageData", "(Ljava/lang/String;)[I");
            final MethodRef allocateAndSetupTexture = new MethodRef(getDeobfClass(), "allocateAndSetupTexture", "(Ljava/awt/image/BufferedImage;)I");

            final int getInputStreamOpcode;
            final JavaRef getInputStream;
            if (haveITexturePack) {
                getInputStreamOpcode = INVOKEINTERFACE;
                getInputStream = new InterfaceMethodRef("ITexturePack", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            } else {
                getInputStreamOpcode = INVOKEVIRTUAL;
                getInputStream = new MethodRef("TexturePackBase", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            }

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("%clamp%")
                    );
                }
            }.setMethod(refreshTextures));

            if (getMinecraftVersion().compareTo("1.0.0") >= 0) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // var1 = -1;
                            begin(),
                            push(-1),
                            ISTORE_1
                        );
                    }
                }.setMethod(updateDynamicTextures));
            } else {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V"))
                        );
                    }
                }.setMethod(updateDynamicTextures));
            }

            addMemberMapper(new FieldMapper(imageData));
            addMemberMapper(new FieldMapper(textureList));
            addMemberMapper(new FieldMapper(clampTexture, blurTexture)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
            addMemberMapper(new MethodMapper(registerTextureFX));
            addMemberMapper(new MethodMapper(readTextureImage));
            addMemberMapper(new MethodMapper(setupTexture));
            addMemberMapper(new MethodMapper(getTexture));
            if (haveGetImageRGB) {
                addMemberMapper(new MethodMapper(getImageRGB));
            }
            if (haveColorizerWater) {
                addMemberMapper(new MethodMapper(readTextureImageData));
            }
            memberMappers.add(new MethodMapper(allocateAndSetupTexture));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    String op = (getCaptureGroup(1)[0] == IREM ? "%" : "/");
                    return String.format("(i %1$s 16) * 16 + j * 16 -> (i %1$s 16) * int_size + j * int_size", op);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(16),
                        capture(subset(new byte[]{IREM, IDIV}, true)),
                        push(16),
                        IMUL,
                        capture(any(1, 3)),
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

            addPatch(new BytecodePatch() {
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

            addPatch(new BytecodePatch() {
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "getInputStream(...), readTextureImage -> getImage(...)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_2,
                        any(0, 6), // some mods have two useless CHECKCASTS here
                        ALOAD_1,
                        reference(getInputStreamOpcode, getInputStream),
                        anyASTORE,
                        anyALOAD,
                        IFNONNULL, any(2),
                        ALOAD_0,
                        ALOAD_0,
                        GETFIELD, any(2),
                        anyILOAD,
                        reference(INVOKEVIRTUAL, setupTexture),
                        GOTO, any(2),
                        ALOAD_0,
                        ALOAD_0,
                        anyALOAD,
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

            addPatch(new BytecodePatch() {
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
                        capture(any(2, 5)),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "put", "([B)Ljava/nio/ByteBuffer;")),
                        POP,

                        // imageData.position(0).limit($1.length);
                        ALOAD_0,
                        reference(GETFIELD, imageData),
                        ICONST_0,
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "position", "(I)Ljava/nio/Buffer;")),
                        backReference(1),
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call TextureUtils.registerTextureFX";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        any(0, 50),
                        end()
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in setupTexture";
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
                        ALOAD_1,
                        IFNONNULL, branch("A"),
                        RETURN,
                        label("A")
                    );
                }
            }.targetMethod(setupTexture));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "null check in getImageRGB";
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
                        ALOAD_1,
                        IFNONNULL, branch("A"),
                        ALOAD_2,
                        ARETURN,
                        label("A")
                    );
                }
            }.targetMethod(getImageRGB));

            addPatch(new TileSizePatch(1048576, "int_glBufferSize"));

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "reloadTextures", "(Lnet/minecraft/client/Minecraft;)V")) {
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

            addPatch(new BytecodePatch.InsertBefore() {
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

            addClassSignature(new FixedBytecodeSignature(
                SIPUSH, 0x04, 0x00, // 1024
                NEWARRAY, T_BYTE
            ));

            addClassSignature(new FixedBytecodeSignature(
                begin(),
                RETURN,
                end()
            ).setMethodName("onTick"));

            addMemberMapper(new FieldMapper(imageData));
            addMemberMapper(new FieldMapper(tileNumber, null, tileSize, tileImage));

            addMemberMapper(new MethodMapper(bindImage));

            addPatch(new TileSizePatch.ArraySizePatch(1024, "int_numBytes"));

            addPatch(new BytecodePatch.InsertBefore() {
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for bindImage recursion (start)";
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
            setParentClass("TextureFX");

            final FieldRef currentAngle = new FieldRef(getDeobfClass(), "currentAngle", "D");
            final FieldRef targetAngle = new FieldRef(getDeobfClass(), "targetAngle", "D");

            addClassSignature(new ConstSignature("/gui/items.png"));
            addClassSignature(new ConstSignature("/misc/dial.png").negate(true));
            addClassSignature(new ConstSignature(new MethodRef("java.lang.Math", "sin", "(D)D")));

            addClassSignature(new FixedBytecodeSignature(
                ALOAD_0,
                SIPUSH, 0x01, 0x00, // 256
                NEWARRAY, T_INT,
                PUTFIELD, any(2),
                ALOAD_0
            ));

            addMemberMapper(new FieldMapper(currentAngle, targetAngle));

            addPatch(new TileSizePatch(7.5, "double_compassCenterMin"));
            addPatch(new TileSizePatch(8.5, "double_compassCenterMax"));
            addPatch(new TileSizePatch.ArraySizePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.MultiplyPatch(16, "int_size"));
            addPatch(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch(-4, "int_compassCrossMin"));
            addPatch(new TileSizePatch.IfGreaterPatch(4, "int_compassCrossMax"));
            addPatch(new TileSizePatch(-8, "int_compassNeedleMin"));
            addPatch(new TileSizePatch.IfGreaterPatch(16, "int_compassNeedleMax"));
            addPatch(new TileSizePatch.GetRGBPatch());
        }
    }

    private class FireMod extends ClassMod {
        FireMod() {
            setParentClass("TextureFX");

            addClassSignature(new ConstSignature(new MethodRef("java.lang.Math", "random", "()D")));

            addClassSignature(new FixedBytecodeSignature(
                SIPUSH, 0x01, 0x40, // 320
                NEWARRAY, T_FLOAT,
                PUTFIELD, any(2),
                ALOAD_0,
                SIPUSH, 0x01, 0x40, // 320
                NEWARRAY, T_FLOAT,
                PUTFIELD, any(2),
                RETURN
            ));

            addPatch(new TileSizePatch(1.06f, "float_flameNudge"));
            addPatch(new TileSizePatch(1.0600001f, "float_flameNudge"));
            addPatch(new TileSizePatch.ArraySizePatch(320, "int_flameArraySize"));
            addPatch(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.WhilePatch(20, "int_flameHeight"));
            addPatch(new TileSizePatch.WhilePatch(16, "int_size"));
            addPatch(new TileSizePatch.ModPatch(20, "int_flameHeight"));
            addPatch(new TileSizePatch.IfLessPatch(19, "int_flameHeightMinus1"));

            addPatch(new TileSizePatch.MultiplyPatch(16, "int_size") {
                @Override
                public boolean filterMethod() {
                    return !getMethodInfo().isConstructor();
                }
            });
        }
    }

    private class FluidMod extends ClassMod {
        private String name;

        FluidMod(String name) {
            setParentClass("TextureFX");

            this.name = name;
            boolean lava = name.contains("Lava");
            boolean flow = name.contains("Flow");

            addClassSignature(new FixedBytecodeSignature(
                ALOAD_0,
                GETSTATIC, any(2),
                GETFIELD, any(2),
                (flow ? new byte[]{ICONST_1, IADD} : new byte[0]),
                INVOKESPECIAL
            ));

            final double rand = (lava ? 0.005 : flow ? 0.2 : 0.05);

            addClassSignature(new BytecodeSignature() {
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
                addClassSignature(new BytecodeSignature() {
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

            addPatch(new TileSizePatch.ArraySizePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.WhilePatch(16, "int_size"));
            addPatch(new TileSizePatch.BitMaskPatch(255, "int_numPixelsMinus1"));
            addPatch(new TileSizePatch.BitMaskPatch(15, "int_sizeMinus1"));
            addPatch(new TileSizePatch.MultiplyPatch(16, "int_size"));
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }

    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            addClassSignature(new ConstSignature(-0.9375F));
            addClassSignature(new ConstSignature(0.0625F));
            addClassSignature(new ConstSignature(0.001953125F));

            addPatch(new TileSizePatch.ToolPixelTopPatch());
            addPatch(new TileSizePatch(16.0F, "float_size"));
            addPatch(new TileSizePatch.WhilePatch(16, "int_size"));
            addPatch(new TileSizePatch.ToolTexPatch());
            addPatch(new TileSizePatch(0.001953125F, "float_texNudge"));
        }
    }

    private class WatchMod extends ClassMod {
        public WatchMod() {
            setParentClass("TextureFX");

            addClassSignature(new ConstSignature("/misc/dial.png"));

            addPatch(new TileSizePatch(16.0D, "double_size"));
            addPatch(new TileSizePatch(15.0D, "double_sizeMinus1"));
            addPatch(new TileSizePatch.GetRGBPatch());
            addPatch(new TileSizePatch.ArraySizePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.MultiplyPatch(16, "int_size"));
            addPatch(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.BitMaskPatch(15, "int_sizeMinus1"));
            addPatch(new TileSizePatch.DivPatch(16, "int_size"));

            addPatch(new TileSizePatch.ModPatch(16, "int_size")
                .addPreMatchSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            push(Math.PI)
                        );
                    }
                })
            );
        }
    }

    private class PortalMod extends ClassMod {
        PortalMod() {
            setParentClass("TextureFX");

            final MethodRef atan2 = new MethodRef("java.lang.Math", "atan2", "(DD)D");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, any(),
                        F2D,
                        FLOAD, any(),
                        F2D,
                        reference(INVOKESTATIC, atan2),
                        D2F
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new TileSizePatch(16.0F, "float_size"));
            addPatch(new TileSizePatch.WhilePatch(16, "int_size"));
            addPatch(new TileSizePatch.ArraySize2DPatch(1024, "int_numBytes", 32));
            addPatch(new TileSizePatch.MultiplyPatch(16, "int_size"));
            addPatch(new TileSizePatch.MultiplyPatch(8, "int_sizeHalf"));
            addPatch(new TileSizePatch.WhilePatch(256, "int_numPixels"));
            addPatch(new TileSizePatch.IfLessPatch(256, "int_numPixels"));
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

            addMemberMapper(new FieldMapper(renderEngine));
        }

        private void addColorizerSignature(final String name) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/misc/" + name.toLowerCase() + "color.png"),
                        anyReference(INVOKEVIRTUAL),
                        captureReference(INVOKESTATIC)
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

            addMemberMapper(new FieldMapper(colorBuffer));

            addPatch(new MakeMemberPublicPatch(colorBuffer));

            if (haveColorizerWater) {
                addPrerequisiteClass("Minecraft");
            } else {
                addClassSignature(new ConstSignature(resource));
            }
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }

    private class GuiContainerCreativeMod extends ClassMod {
        GuiContainerCreativeMod() {
            setMultipleMatchesAllowed(true);

            addClassSignature(new ConstSignature("/gui/allitems.png"));

            addPatch(new BytecodePatch.InsertBefore() {
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
