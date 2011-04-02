package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.*;

import java.io.IOException;
import java.io.InputStream;

import static javassist.bytecode.Opcode.*;

public class HDTextureMod extends Mod {
    public HDTextureMod() {
        name = "HD Textures";
        author = "MCPatcher";
        description = "Provides support for texture packs of 32x32, 64x64, 128x128, and 256x256.";
        version = "1.0";

        allowedDirs.clear();
        allowedDirs.add("");
        allowedDirs.add("net/minecraft/client");

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
        classMods.add(new GLAllocationMod());
        classMods.add(new TexturePackListMod());
        classMods.add(new TexturePackBaseMod());
        classMods.add(new TexturePackDefaultMod());
        classMods.add(new FontRendererMod());
        classMods.add(new GameSettingsMod());
        classMods.add(new GetResourceMod());
        classMods.add(new ColorizerMod("ColorizerFoliage", "/misc/foliagecolor.png"));
        classMods.add(new ColorizerMod("ColorizerGrass", "/misc/grasscolor.png"));

        filesToAdd.add("TileSize.class");
        filesToAdd.add("TextureUtils.class");
        filesToAdd.add("CustomAnimation.class");
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        return MCPatcher.class.getResourceAsStream("/newcode/" + name);
    }

    private static class RenderEngineMod extends ClassMod {
        public RenderEngineMod() {
            classSignatures.add(new ConstSignature(new MethodRef("org.lwjgl.opengl.GL11", "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

            fieldMappers.add(new FieldMapper("imageData", "Ljava/nio/ByteBuffer;"));
            fieldMappers.add(new FieldMapper("textureList", "Ljava/util/List;"));

            methodMappers.add(new MethodMapper("registerTextureFX", "(LTextureFX;)V"));
            methodMappers.add(new MethodMapper("refreshTextures", "()V") {
                @Override
                public boolean match(MethodInfo methodInfo) {
                    if (!super.match(methodInfo) || methodInfo.getName().startsWith("<")) {
                        return false;
                    }
                    BytecodeMatcher bm = new BytecodeMatcher(
                        push(methodInfo, "%clamp%")
                    );
                    return bm.match(methodInfo);
                }
            });
            methodMappers.add(new MethodMapper("readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    String op = (getCaptureGroup(1)[0] == IREM ? "%" : "/");
                    return String.format("(i %1$s 16) * 16 + j * 16 -> (i %1$s 16) * int_size + j * int_size", op);
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 16),
                        BinaryRegex.capture(BinaryRegex.subset(new byte[]{IREM, IDIV}, true)),
                        push(methodInfo, 16),
                        IMUL,
                        BinaryRegex.capture(BinaryRegex.repeat(BinaryRegex.any(), 1, 3)),
                        push(methodInfo, 16),
                        IMUL
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        push(methodInfo, 16),
                        getCaptureGroup(1),
                        reference(methodInfo, GETSTATIC, new FieldRef("TileSize", "int_size", "I")),
                        IMUL,
                        getCaptureGroup(2),
                        reference(methodInfo, GETSTATIC, new FieldRef("TileSize", "int_size", "I")),
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 16),
                        push(methodInfo, 16),
                        SIPUSH, 0x19, 0x08, // GL_RGBA
                        SIPUSH, 0x14, 0x01  // GL_UNSIGNED_BYTE
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, GETSTATIC, new FieldRef("TileSize", "int_size", "I")),
                        reference(methodInfo, GETSTATIC, new FieldRef("TileSize", "int_size", "I")),
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, map(new MethodRef("TexturePackBase", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;"))),
                        reference(methodInfo, INVOKESPECIAL, map(new MethodRef("RenderEngine", "readTextureImage", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;")))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        SWAP,
                        POP,
                        SWAP,
                        POP,
                        reference(methodInfo, INVOKESTATIC, map(new MethodRef("TextureUtils", "getResourceAsBufferedImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;")))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "imageData.clear(), .put(), .limit() -> imageData = TextureUtils.getByteBuffer()";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // imageData.clear();
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, map(new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;"))),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP,

                        // imageData.put($1);
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, map(new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;"))),
                        BinaryRegex.capture(BinaryRegex.repeat(BinaryRegex.any(), 2, 5)),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "put", "([B)Ljava/nio/ByteBuffer;")),
                        POP,

                        // imageData.position(0).limit($1.length);
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, map(new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;"))),
                        ICONST_0,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "position", "(I)Ljava/nio/Buffer;")),
                        BinaryRegex.backReference(1),
                        ARRAYLENGTH,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/Buffer", "limit", "(I)Ljava/nio/Buffer;")),
                        POP
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // imageData = TextureUtils.getByteBuffer(imageData, $1);
                        ALOAD_0,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, map(new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;"))),
                        getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "getByteBuffer", "(Ljava/nio/ByteBuffer;[B)Ljava/nio/ByteBuffer;")),
                        reference(methodInfo, PUTFIELD, map(new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;")))
                    );
                }
            });

            patches.add(new BytecodeTilePatch(1048576, "int_glBufferSize"));

            patches.add(new AddMethodPatch("setTileSize", "(Lnet/minecraft/client/Minecraft;)V") {
                @Override
                public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws IOException {
                    maxStackSize = 10;
                    numLocals = 5;
                    return buildCode(
                        // imageData = GLAllocation.createDirectByteBuffer(TileSize.int_glBufferSize);
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef("TileSize", "int_glBufferSize", "I")),
                        reference(methodInfo, INVOKESTATIC, map(new MethodRef("GLAllocation", "createDirectByteBuffer", "(I)Ljava/nio/ByteBuffer;"))),
                        reference(methodInfo, PUTFIELD, map(new FieldRef("RenderEngine", "imageData", "Ljava/nio/ByteBuffer;"))),

                        // refreshTextures();
                        ALOAD_0,
                        reference(methodInfo, INVOKEVIRTUAL, map(new MethodRef("RenderEngine", "refreshTextures", "()V"))),

                        // TextureUtils.refreshTextureFX(textureList);
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, map(new FieldRef("RenderEngine", "textureList", "Ljava/util/List;"))),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "refreshTextureFX", "(Ljava/util/List;)V")),

                        RETURN
                    );
                }
            });
        }
    }

    private static class TextureFXMod extends ClassMod {
        public TextureFXMod() {
            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0x04, 0x00, // 1024
                NEWARRAY, T_BYTE
            ));
            classSignatures.add(new FixedBytecodeSignature(
                "^", RETURN, "$"
            ).setMethodName("onTick"));

            fieldMappers.add(new FieldMapper("imageData", "[B"));
            fieldMappers.add(new FieldMapper("", "I") {
                private int fieldNum = 0;

                @Override
                public boolean match(FieldInfo fieldInfo) {
                    if (!fieldInfo.getDescriptor().equals(descriptor)) {
                        return false;
                    }
                    switch (fieldNum) {
                        case 0:
                            name = "tileNumber";
                            break;
                        case 1:
                            name = "field_1130_d";
                            break;
                        case 2:
                            name = "tileSize";
                            break;
                        case 3:
                            name = "tileImage";
                            break;
                        default:
                            return false;
                    }
                    fieldNum++;
                    return true;
                }
            });

            patches.add(new BytecodeTilePatch.ArraySizePatch(1024, "int_numBytes"));
        }

        @Override
        public boolean mapClassMembers(String filename, ClassFile classFile) throws Exception {
            if (!super.mapClassMembers(filename, classFile)) {
                return false;
            }
            mod.getClassMap().addInheritance("TextureFX", "CustomAnimation");
            return true;
        }
    }

    private static class CompassMod extends ClassMod {
        public CompassMod() {
            classSignatures.add(new ConstSignature("/gui/items.png"));
            classSignatures.add(new ConstSignature("/misc/dial.png").negate(true));
            classSignatures.add(new ConstSignature(new MethodRef("java.lang.Math", "sin", "(D)D")));
            classSignatures.add(new FixedBytecodeSignature(
                ALOAD_0,
                SIPUSH, 0x01, 0x00, // 256
                NEWARRAY, T_INT,
                PUTFIELD, BinaryRegex.repeat(BinaryRegex.any(), 2),
                ALOAD_0
            ));

            patches.add(new BytecodeTilePatch(7.5, "double_compassCenterMin"));
            patches.add(new BytecodeTilePatch(8.5, "double_compassCenterMax"));
            patches.add(new BytecodeTilePatch.ArraySizePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.MultiplyPatch(16, "int_size"));
            patches.add(new BytecodeTilePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch(-4, "int_compassCrossMin"));
            patches.add(new BytecodeTilePatch.IfGreaterPatch(4, "int_compassCrossMax"));
            patches.add(new BytecodeTilePatch(-8, "int_compassNeedleMin"));
            patches.add(new BytecodeTilePatch.IfGreaterPatch(16, "int_compassNeedleMax"));

            patches.add(new BytecodeTilePatch.GetRGBPatch());
        }
    }

    private static class FireMod extends ClassMod {
        public FireMod() {
            classSignatures.add(new ConstSignature(new MethodRef("java.lang.Math", "random", "()D")));
            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0x01, 0x40, // 320
                NEWARRAY, T_FLOAT,
                PUTFIELD, BinaryRegex.repeat(BinaryRegex.any(), 2),
                ALOAD_0,
                SIPUSH, 0x01, 0x40, // 320
                NEWARRAY, T_FLOAT,
                PUTFIELD, BinaryRegex.repeat(BinaryRegex.any(), 2),
                RETURN
            ));

            patches.add(new BytecodeTilePatch(1.06F, "float_flameNudge"));
            patches.add(new BytecodeTilePatch.ArraySizePatch(320, "int_flameArraySize"));
            patches.add(new BytecodeTilePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.WhilePatch(20, "int_flameHeight"));
            patches.add(new BytecodeTilePatch.WhilePatch(16, "int_size"));
            patches.add(new BytecodeTilePatch.MultiplyPatch(16, "int_size") {
                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    return !methodInfo.getName().equals("<init>");
                }
            });
            patches.add(new BytecodeTilePatch.ModPatch(20, "int_flameHeight"));
            patches.add(new BytecodeTilePatch.IfLessPatch(19, "int_flameHeightMinus1"));
        }
    }

    private static class FluidMod extends ClassMod {
        private String name;

        public FluidMod(String name) {
            this.name = name;
            boolean lava = name.contains("Lava");
            boolean flow = name.contains("Flow");

            classSignatures.add(new FixedBytecodeSignature(
                ALOAD_0,
                GETSTATIC, BinaryRegex.repeat(BinaryRegex.any(), 2),
                GETFIELD, BinaryRegex.repeat(BinaryRegex.any(), 2),
                (flow ? new byte[]{ICONST_1, IADD} : new byte[0]),
                INVOKESPECIAL
            ));

            final double rand = (lava ? 0.005 : flow ? 0.2 : 0.05);
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef("java.lang.Math", "random", "()D")),
                        push(methodInfo, rand),
                        DCMPG,
                        IFGE
                    );
                }
            });

            if (lava) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression(MethodInfo methodInfo) {
                        return buildExpression(
                            I2F,
                            push(methodInfo, 3.1415927F),
                            FMUL,
                            FCONST_2,
                            FMUL,
                            push(methodInfo, 16.0F),
                            FDIV
                        );
                    }
                });
            }

            patches.add(new BytecodeTilePatch.ArraySizePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.WhilePatch(16, "int_size"));
            patches.add(new BytecodeTilePatch.BitMaskPatch(255, "int_numPixelsMinus1"));
            patches.add(new BytecodeTilePatch.BitMaskPatch(15, "int_sizeMinus1"));
            patches.add(new BytecodeTilePatch.MultiplyPatch(16, "int_size"));
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }

    private static class ItemRendererMod extends ClassMod {
        public ItemRendererMod() {
            classSignatures.add(new ConstSignature(-0.9375F));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "tool pixel top";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        LDC, BinaryRegex.any(),
                        FADD,
                        BinaryRegex.capture(buildExpression(
                            FSTORE, BinaryRegex.any(),
                            ALOAD_2,
                            DCONST_0
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1)
                    );
                }
            });

            patches.add(new BytecodeTilePatch(16.0F, "float_size"));
            patches.add(new BytecodeTilePatch.WhilePatch(16, "int_size"));
            patches.add(new BytecodeTilePatch(1.0F / 512.0F, "float_texNudge"));
        }
    }

    private static class WatchMod extends ClassMod {
        public WatchMod() {
            classSignatures.add(new ConstSignature("/misc/dial.png"));

            patches.add(new BytecodeTilePatch(16.0D, "double_size"));
            patches.add(new BytecodeTilePatch(15.0D, "double_sizeMinus1"));
            patches.add(new BytecodeTilePatch.GetRGBPatch());
            patches.add(new BytecodeTilePatch.ArraySizePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.MultiplyPatch(16, "int_size"));
            patches.add(new BytecodeTilePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.BitMaskPatch(15, "int_sizeMinus1"));
            patches.add(new BytecodeTilePatch.ModPatch(16, "int_size") {
                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    return !methodInfo.getName().equals("<init>");
                }
            });
            patches.add(new BytecodeTilePatch.DivPatch(16, "int_size"));
        }
    }

    private static class PortalMod extends ClassMod {
        public PortalMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        FLOAD, BinaryRegex.any(),
                        F2D,
                        FLOAD, BinaryRegex.any(),
                        F2D,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("java.lang.Math", "atan2", "(DD)D")),
                        D2F
                    );
                }
            });

            patches.add(new BytecodeTilePatch(16.0F, "float_size"));
            patches.add(new BytecodeTilePatch.WhilePatch(16, "int_size"));
            patches.add(new BytecodeTilePatch.ArraySize2DPatch(1024, "int_numBytes", 32));
            patches.add(new BytecodeTilePatch.MultiplyPatch(16, "int_size"));
            patches.add(new BytecodeTilePatch.MultiplyPatch(8, "int_sizeHalf"));
            patches.add(new BytecodeTilePatch.WhilePatch(256, "int_numPixels"));
            patches.add(new BytecodeTilePatch.IfLessPatch(256, "int_numPixels"));
        }
    }

    private static class MinecraftMod extends ClassMod {
        public MinecraftMod() {
            classSignatures.add(new FilenameSignature("net/minecraft/client/Minecraft.class"));

            fieldMappers.add(new FieldMapper("texturePackList", "LTexturePackList;"));
            fieldMappers.add(new FieldMapper("renderEngine", "LRenderEngine;"));
            fieldMappers.add(new FieldMapper("gameSettings", "LGameSettings;"));
            fieldMappers.add(new FieldMapper("fontRenderer", "LFontRenderer;"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "TextureUtils.setMinecraft(this) on startup";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(buildExpression(
                        ALOAD_0,
                        reference(methodInfo, NEW, map(new ClassRef("TexturePackList"))),
                        BinaryRegex.repeat(BinaryRegex.any(), 0, 18),
                        PUTFIELD, BinaryRegex.any(), BinaryRegex.any()
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "setMinecraft", "(Lnet/minecraft/client/Minecraft;)V"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "TextureUtils.setTileSize(), renderEngine.setTileSize() on startup";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(buildExpression(
                        ALOAD_0,
                        reference(methodInfo, NEW, map(new ClassRef("RenderEngine"))),
                        BinaryRegex.repeat(BinaryRegex.any(), 0, 18),
                        PUTFIELD, BinaryRegex.capture(BinaryRegex.repeat(BinaryRegex.any(), 2))
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "setTileSize", "()Z")),
                        POP,
                        ALOAD_0,
                        GETFIELD, getCaptureGroup(2),
                        ALOAD_0,
                        reference(methodInfo, INVOKEVIRTUAL, map(new MethodRef("RenderEngine", "setTileSize", "(LMinecraft;)V")))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                private JavaRef registerTextureFX = null;

                @Override
                public String getDescription() {
                    return "remove registerTextureFX call";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (registerTextureFX == null) {
                        registerTextureFX = map(new MethodRef("RenderEngine", "registerTextureFX", "(LTextureFX;)V"));
                    }

                    return buildExpression(
                        ALOAD_0,
                        GETFIELD, // RenderEngine
                        BinaryRegex.repeat(BinaryRegex.any(), 0, 12),
                        reference(methodInfo, INVOKEVIRTUAL, registerTextureFX)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return new byte[0];
                }
            });
        }
    }

    private static class GLAllocationMod extends ClassMod {
        public GLAllocationMod() {
            classSignatures.add(new ConstSignature(
                new MethodRef("org.lwjgl.opengl.ARBShaderObjects", "glCreateProgramObjectARB", "()I")
            ).negate(true)); // don't match GLSL Shader Mod
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef("java.nio.ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;"))
                    );
                }
            }.setMethodName("createDirectByteBuffer"));
        }
    }

    private static class TexturePackListMod extends ClassMod {
        public TexturePackListMod() {
            classSignatures.add(new ConstSignature(".zip"));
            classSignatures.add(new ConstSignature("texturepacks"));

            fieldMappers.add(new FieldMapper("selectedTexturePack", "LTexturePackBase;") {
                @Override
                public boolean match(FieldInfo fieldInfo) {
                    return super.match(fieldInfo) && (fieldInfo.getAccessFlags() & AccessFlag.PUBLIC) != 0;
                }
            });
            fieldMappers.add(new FieldMapper("defaultTexturePack", "LTexturePackBase;") {
                @Override
                public boolean match(FieldInfo fieldInfo) {
                    return super.match(fieldInfo) && (fieldInfo.getAccessFlags() & AccessFlag.PRIVATE) != 0;
                }
            });
            fieldMappers.add(new FieldMapper("minecraft", "LMinecraft;"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "TexturePackList.setTileSize(selectedTexturePack) on texture pack change";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.capture(buildExpression(
                            ALOAD_0,
                            reference(methodInfo, GETFIELD, map(new FieldRef("TexturePackList", "selectedTexturePack", "LTexturePackBase;"))),
                            INVOKEVIRTUAL, BinaryRegex.repeat(BinaryRegex.any(), 2)
                        )),
                        BinaryRegex.capture(buildExpression(
                            ICONST_1,
                            IRETURN
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "setTileSize", "()Z")),
                        POP,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, map(new FieldRef("TexturePackList", "minecraft", "LMinecraft;"))),
                        DUP,
                        reference(methodInfo, GETFIELD, map(new FieldRef("Minecraft", "renderEngine", "LRenderEngine;"))),
                        SWAP,
                        reference(methodInfo, INVOKEVIRTUAL, map(new MethodRef("RenderEngine", "setTileSize", "(LMinecraft;)V"))),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "setFontRenderer", "()V")),
                        getCaptureGroup(2)
                    );
                }
            });
        }
    }

    private static class TexturePackBaseMod extends ClassMod {
        public TexturePackBaseMod() {
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            classSignatures.add(new ConstSignature(getResourceAsStream));
            classSignatures.add(new ConstSignature("pack.txt").negate(true));
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_1,
                        reference(methodInfo, INVOKEVIRTUAL, getResourceAsStream),
                        ARETURN
                    );
                }
            }.setMethodName("getInputStream"));
        }
    }

    private static class TexturePackDefaultMod extends ClassMod {
        public TexturePackDefaultMod() {
            classSignatures.add(new ConstSignature("The default look of Minecraft"));
        }
    }

    private static class FontRendererMod extends ClassMod {
        public FontRendererMod() {
            classSignatures.add(new FixedBytecodeSignature(
                DCONST_0,
                DCONST_0,
                DCONST_0,
                ILOAD
            ));
            classSignatures.add(new FixedBytecodeSignature(
                ALOAD, BinaryRegex.any(),
                ICONST_0,
                ICONST_0,
                ILOAD, BinaryRegex.any(),
                ILOAD, BinaryRegex.any(),
                ALOAD, BinaryRegex.any(),
                ICONST_0,
                ILOAD, BinaryRegex.any(),
                INVOKEVIRTUAL
            ));

            patches.add(new AddMethodPatch("initialize", "()V") {
                @Override
                public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) {
                    MethodInfo constructor = classFile.getMethod("<init>");
                    CodeAttribute ca = constructor.getCodeAttribute();
                    methodInfo.setDescriptor(constructor.getDescriptor());
                    maxStackSize = ca.getMaxStack();
                    numLocals = ca.getMaxLocals();
                    exceptionTable = ca.getExceptionTable();
                    byte[] code = ca.getCode().clone();
                    code[0] = NOP;  // remove java.lang.Object<init> call
                    code[1] = NOP;
                    code[2] = NOP;
                    code[3] = NOP;
                    return code;
                }
            });
        }
    }

    private static class GameSettingsMod extends ClassMod {
        public GameSettingsMod() {
            classSignatures.add(new ConstSignature("key.forward"));
        }
    }

    private static class GetResourceMod extends ClassMod {
        public GetResourceMod() {
            global = true;

            classSignatures.add(new ConstSignature(new ClassRef("java.lang.Class")));
            classSignatures.add(new ConstSignature(new MethodRef("javax.imageio.ImageIO", "read", null)));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "ImageIO.read(getResource(...)) -> getResourceAsBufferedImage(...)";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        LDC, BinaryRegex.any(),
                        BinaryRegex.capture(BinaryRegex.or(
                            buildExpression(LDC, BinaryRegex.any()),
                            buildExpression(ALOAD, BinaryRegex.any()),
                            buildExpression(BinaryRegex.subset(new byte[]{ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3}, true))
                        )),
                        BinaryRegex.or(
                            buildExpression(
                                reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java.lang.Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;")),
                                reference(methodInfo, INVOKESTATIC, new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/net/URL;)Ljava/awt/image/BufferedImage;"))
                            ),
                            buildExpression(
                                reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;")),
                                reference(methodInfo, INVOKESTATIC, new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"))
                            )
                        )
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("TextureUtils", "getResourceAsBufferedImage", "(Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });
        }
    }

    private static class ColorizerMod extends ClassMod {
        private String name;

        public ColorizerMod(String name, String resource) {
            this.name = name;
            classSignatures.add(new ConstSignature(resource));

            fieldMappers.add(new FieldMapper("colorBuffer", "[I"));

            patches.add(new MakeMemberPublicPatch(new FieldRef(name, "colorBuffer", "[I")));
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }
}
