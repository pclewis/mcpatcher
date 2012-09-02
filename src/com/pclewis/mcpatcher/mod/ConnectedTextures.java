package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import static javassist.bytecode.Opcode.*;

public class ConnectedTextures extends Mod {
    public ConnectedTextures(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.CONNECTED_TEXTURES;
        author = "MCPatcher";
        description = "Connects adjacent blocks of the same type.";
        version = "1.4";

        addDependency(BaseTexturePackMod.NAME);

        configPanel = new ConfigPanel();

        classMods.add(new MinecraftMod());
        classMods.add(new RenderEngineMod());
        classMods.add(new BaseMod.IBlockAccessMod());
        classMods.add(new BlockMod());
        classMods.add(new TessellatorMod(minecraftVersion));
        classMods.add(new RenderBlocksMod());
        classMods.add(new WorldRendererMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CTM_UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CTM_UTILS_CLASS + "$1"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.SUPER_TESSELLATOR_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$CTM"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Random1"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Fixed"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Horizontal"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Vertical"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Top"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Repeat"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS));

        getClassMap().addInheritance("Tessellator", MCPatcherUtils.SUPER_TESSELLATOR_CLASS);
    }

    private class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox glassCheckBox;
        private JCheckBox glassPaneCheckBox;
        private JCheckBox bookshelfCheckBox;
        private JCheckBox sandstoneCheckBox;
        private JCheckBox standardCheckBox;
        private JCheckBox nonStandardCheckBox;
        private JCheckBox outlineCheckBox;

        public ConfigPanel() {
            glassCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "glass", glassCheckBox.isSelected());
                }
            });

            glassPaneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", glassPaneCheckBox.isSelected());
                }
            });

            bookshelfCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", bookshelfCheckBox.isSelected());
                }
            });

            sandstoneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", sandstoneCheckBox.isSelected());
                }
            });

            standardCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "standard", standardCheckBox.isSelected());
                }
            });

            nonStandardCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", nonStandardCheckBox.isSelected());
                }
            });

            outlineCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "outline", outlineCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            glassCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glass", true));
            glassPaneCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", true));
            bookshelfCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", true));
            sandstoneCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", true));
            standardCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true));
            nonStandardCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true));
            outlineCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "outline", false));
        }

        @Override
        public void save() {
        }
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");

            memberMappers.add(new FieldMapper(renderEngine));
        }
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
            final MethodRef allocateAndSetupTexture = new MethodRef(getDeobfClass(), "allocateAndSetupTexture", "(Ljava/awt/image/BufferedImage;)I");
            final MethodRef getTexture = new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)I");

            classSignatures.add(new ConstSignature("%clamp%"));
            classSignatures.add(new ConstSignature("%blur%"));
            classSignatures.add(new ConstSignature(glTexSubImage2D));

            memberMappers.add(new MethodMapper(getTexture)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            memberMappers.add(new MethodMapper(allocateAndSetupTexture)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            final MethodRef getBlockTexture = new MethodRef(getDeobfClass(), "getBlockTexture", "(LIBlockAccess;IIII)I");
            final InterfaceMethodRef getBlockMetadata = new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I");
            final MethodRef getBlockTextureFromSideAndMetadata = new MethodRef(getDeobfClass(), "getBlockTextureFromSideAndMetadata", "(II)I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        ILOAD, 5,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEINTERFACE),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        IRETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(getBlockTexture)
                .addXref(1, getBlockMetadata)
                .addXref(2, getBlockTextureFromSideAndMetadata)
            );
        }
    }

    private class TessellatorMod extends BaseMod.TessellatorMod {
        TessellatorMod(MinecraftVersion minecraftVersion) {
            super(minecraftVersion);

            final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(I)V");
            final MethodRef constructor0 = new MethodRef(getDeobfClass(), "<init>", "()V");
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final FieldRef isDrawing = new FieldRef(getDeobfClass(), "isDrawing", "Z");
            final FieldRef drawMode = new FieldRef(getDeobfClass(), "drawMode", "I");
            final FieldRef texture = new FieldRef(getDeobfClass(), "texture", "I");
            final FieldRef bufferSize = new FieldRef(getDeobfClass(), "bufferSize", "I");
            final FieldRef addedVertices = new FieldRef(getDeobfClass(), "addedVertices", "I");
            final FieldRef vertexCount = new FieldRef(getDeobfClass(), "vertexCount", "I");
            final FieldRef rawBufferIndex = new FieldRef(getDeobfClass(), "rawBufferIndex", "I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        push(4),
                        IREM,

                        BinaryRegex.any(0, 1000),

                        ALOAD_0,
                        DUP,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        BytecodeMatcher.anyReference(PUTFIELD),

                        ALOAD_0,
                        DUP,
                        BytecodeMatcher.captureReference(GETFIELD),
                        push(8),
                        IADD,
                        BytecodeMatcher.anyReference(PUTFIELD)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, addedVertices)
                .addXref(2, vertexCount)
                .addXref(3, rawBufferIndex)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),

                        BinaryRegex.any(0, 50),

                        push("Already tesselating!"),
                        BinaryRegex.any(0, 100),

                        ALOAD_0,
                        BytecodeMatcher.captureReference(INVOKESPECIAL),

                        ALOAD_0,
                        ILOAD_1,
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(startDrawing)
                .addXref(1, isDrawing)
                .addXref(2, reset)
                .addXref(3, drawMode)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        push(7),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        RETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, bufferSize));

            memberMappers.add(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));

            for (JavaRef ref : new JavaRef[]{constructor, startDrawing, isDrawing, drawMode, draw, reset, bufferSize,
                addedVertices, vertexCount, rawBufferIndex}) {
                patches.add(new MakeMemberPublicPatch(ref));
            }

            patches.add(new AddFieldPatch(texture));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace tessellator instance";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isStaticInitializer()) {
                        return buildExpression(
                            reference(NEW, new ClassRef("Tessellator")),
                            DUP,
                            BinaryRegex.capture(BinaryRegex.optional(BytecodeMatcher.anyLDC)),
                            BinaryRegex.capture(BinaryRegex.or(
                                BinaryRegex.build(reference(INVOKESPECIAL, constructor)),
                                BinaryRegex.build(reference(INVOKESPECIAL, constructor0))
                            ))
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    boolean isForge = getCaptureGroup(1).length == 0;
                    return buildCode(
                        reference(NEW, new ClassRef(MCPatcherUtils.SUPER_TESSELLATOR_CLASS)),
                        DUP,
                        getCaptureGroup(1),
                        reference(INVOKESPECIAL, new MethodRef(MCPatcherUtils.SUPER_TESSELLATOR_CLASS, "<init>", isForge ? "()V" : "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "initialize texture field to -1";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        push(-1),
                        reference(PUTFIELD, texture)
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "bind texture before drawing";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.or(
                            BinaryRegex.build(
                                ALOAD_0,
                                BytecodeMatcher.anyReference(GETFIELD)
                            ),
                            BytecodeMatcher.anyReference(GETSTATIC)
                        ),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/IntBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (texture >= 0) {
                        ALOAD_0,
                        reference(GETFIELD, texture),
                        IFLT, branch("A"),

                        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                        push(3553), // GL11.GL_TEXTURE_2D
                        ALOAD_0,
                        reference(GETFIELD, texture),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V")),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(draw));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "fix references to reset method";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESPECIAL, reset)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKEVIRTUAL, reset)
                    );
                }
            });
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];
        private final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "I");
        private final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        private final FieldRef instance = new FieldRef("Tessellator", "instance", "LTessellator;");
        private final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;III)Z");
        private final MethodRef drawCrossedSquares;
        private final MethodRef renderBlockPane = new MethodRef(getDeobfClass(), "renderBlockPane", "(LBlockPane;III)Z");
        private final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
        private final MethodRef setup = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "setup", "(LRenderBlocks;LBlock;IIIII)Z");
        private final MethodRef setupNoFace = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "setup", "(LRenderBlocks;LBlock;IIII)Z");
        private final MethodRef reset = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "reset", "()V");
        private final FieldRef newTextureIndex = new FieldRef(MCPatcherUtils.CTM_UTILS_CLASS, "newTextureIndex", "I");
        private final FieldRef newTessellator = new FieldRef(MCPatcherUtils.CTM_UTILS_CLASS, "newTessellator", "LTessellator;");

        private ArrayList<MethodInfo> renderMethods = new ArrayList<MethodInfo>();
        private ArrayList<Integer> tessellatorRegisters = new ArrayList<Integer>();
        private int[] sideUVRegisters;

        RenderBlocksMod() {
            if (getMinecraftVersion().compareTo("12w34a") >= 0) {
                drawCrossedSquares = new MethodRef(getDeobfClass(), "drawCrossedSquares", "(LBlock;IDDDF)V");
            } else {
                drawCrossedSquares = new MethodRef(getDeobfClass(), "drawCrossedSquares", "(LBlock;IDDD)V");
            }

            setupBlockFace(0, "Bottom");
            setupBlockFace(1, "Top");
            setupBlockFace(2, "North");
            setupBlockFace(3, "South");
            setupBlockFace(4, "West");
            setupBlockFace(5, "East");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // renderStandardBlock(par1BlockBrewingStand, par2, par3, par4);
                        ALOAD_0,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        POP,

                        // overrideBlockTexture = 156;
                        ALOAD_0,
                        push(156),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .addXref(1, renderStandardBlock)
                .addXref(2, overrideBlockTexture)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 5,
                        push(18),
                        BytecodeMatcher.IF_ICMPNE_or_IF_ICMPEQ, BinaryRegex.any(2),

                        ALOAD_0,
                        ALOAD_1,
                        BytecodeMatcher.anyReference(CHECKCAST),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        IRETURN
                    );
                }
            }
                .addXref(1, renderBlockPane)
            );

            memberMappers.add(new FieldMapper(blockAccess));
            memberMappers.add(new MethodMapper(faceMethods));
            memberMappers.add(new MethodMapper(drawCrossedSquares));

            patches.add(new BytecodePatch() {
                private JavaRef ref;

                @Override
                public String getDescription() {
                    return "find potential render methods";
                }

                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    if (ref == null) {
                        ref = map(renderBlockPane);
                    }
                    return methodInfo.getDescriptor().matches("^\\(L[a-z]+;III.*") &&
                        (!methodInfo.getDescriptor().equals(ref.getType()) || !methodInfo.getName().equals(ref.getName()));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, instance),
                        ASTORE, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    renderMethods.add(getMethodInfo());
                    tessellatorRegisters.add(getCaptureGroup(1)[0] & 0xff);
                    return null;
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                private int tessellatorRegister;

                @Override
                public String getDescription() {
                    return "override texture (other blocks)";
                }

                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    for (int i = 0; i < renderMethods.size(); i++) {
                        MethodInfo m = renderMethods.get(i);
                        if (methodInfo.getName().equals(m.getName()) &&
                            methodInfo.getDescriptor().equals(m.getDescriptor())) {
                            tessellatorRegister = tessellatorRegisters.get(i);
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // j = (i & 0x0f) << 4;
                        ILOAD, BinaryRegex.capture(BinaryRegex.any()),
                        push(0x0f),
                        IAND,
                        push(4),
                        ISHL,
                        ISTORE, BinaryRegex.any(),

                        // k = (i & 0xf0);
                        ILOAD, BinaryRegex.backReference(1),
                        push(0xf0),
                        IAND,
                        ISTORE, BinaryRegex.any()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (overrideBlockTexture < 0
                        ALOAD_0,
                        reference(GETFIELD, overrideBlockTexture),
                        IFGE, branch("A"),

                        // && CTMUtils.setup(this, block, i, j, k, texture)) {
                        ALOAD_0,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ILOAD, getCaptureGroup(1),
                        reference(INVOKESTATIC, setupNoFace),
                        IFEQ, branch("A"),

                        // texture = CTMUtils.newTextureIndex;
                        reference(GETSTATIC, newTextureIndex),
                        ISTORE, getCaptureGroup(1),

                        // tessellator = CTMUtils.newTessellator;
                        reference(GETSTATIC, newTessellator),
                        ASTORE, tessellatorRegister,

                        // }
                        label("A")
                    );
                }
            });

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override texture (crossed squares)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator = Tessellator.instance;
                        reference(GETSTATIC, instance),
                        ASTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // i = par1Block.getBlockTextureFromSideAndMetadata(0, par2);
                        ALOAD_1,
                        ICONST_0,
                        ILOAD_2,
                        BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                        ISTORE, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // tessellator = Tessellator.instance;
                        // if (overrideBlockTexture < 0
                        ALOAD_0,
                        reference(GETFIELD, overrideBlockTexture),
                        IFGE, branch("A"),

                        // && CTMUtils.setup(this, block, (int) x, (int) y, (int) z, texture)) {
                        ALOAD_0,
                        ALOAD_1,
                        DLOAD_3,
                        D2I,
                        DLOAD, 5,
                        D2I,
                        DLOAD, 7,
                        D2I,
                        ILOAD, getCaptureGroup(2),
                        reference(INVOKESTATIC, setupNoFace),
                        IFEQ, branch("A"),

                        // texture = CTMUtils.newTextureIndex;
                        reference(GETSTATIC, newTextureIndex),
                        ISTORE, getCaptureGroup(2),

                        // tessellator = CTMUtils.newTessellator;
                        reference(GETSTATIC, newTessellator),
                        ASTORE, getCaptureGroup(1),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(drawCrossedSquares));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "determine glass side texture uv registers";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // sideU0 = (sideU + 7) / 256.0f;
                        BytecodeMatcher.anyILOAD,
                        push(7),
                        IADD,
                        I2F,
                        push(256.0f),
                        FDIV,
                        F2D,
                        DSTORE, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    int reg = getCaptureGroup(1)[0] & 0xff;
                    sideUVRegisters = new int[]{reg, reg + 2, reg + 4, reg + 6, reg + 8};
                    Logger.log(Logger.LOG_CONST, "glass side texture uv registers (%d %d %d %d %d)",
                        reg, reg + 2, reg + 4, reg + 6, reg + 8
                    );
                    return null;
                }
            }.targetMethod(renderBlockPane));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override texture (glass pane)";
                }

                @Override
                public String getMatchExpression() {
                    if (sideUVRegisters == null) {
                        return null;
                    } else {
                        return buildExpression(
                            // connectEast = par1BlockPane.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(i + 1, j, k));
                            ALOAD_1,
                            ALOAD_0,
                            reference(GETFIELD, blockAccess),
                            ILOAD_2,
                            push(1),
                            IADD,
                            ILOAD_3,
                            ILOAD, 4,
                            BytecodeMatcher.anyReference(INVOKEINTERFACE),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                            ISTORE, BinaryRegex.capture(BinaryRegex.any())
                        );
                    }
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    int reg = getCaptureGroup(1)[0] & 0xff;
                    return buildCode(
                        // GlassPaneRenderer.render(renderBlocks, overrideBlockTexture, blockPane, i, j, k, connectNorth, ...);
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, overrideBlockTexture),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ILOAD, reg - 3,
                        ILOAD, reg - 2,
                        ILOAD, reg - 1,
                        ILOAD, reg,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "render", "(LRenderBlocks;ILBlock;IIIZZZZ)V"))
                    );
                }
            }.targetMethod(renderBlockPane));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "disable default rendering (glass pane faces)";
                }

                @Override
                public String getMatchExpression() {
                    if (sideUVRegisters == null) {
                        return null;
                    } else {
                        return buildExpression(BinaryRegex.repeat(BinaryRegex.build(
                            ALOAD, BinaryRegex.any(),
                            BinaryRegex.nonGreedy(BinaryRegex.any(0, 15)),
                            DLOAD, BinaryRegex.subset(sideUVRegisters, false),
                            DLOAD, BinaryRegex.subset(sideUVRegisters, false),
                            reference(INVOKEVIRTUAL, addVertexWithUV)
                        ), 8));
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (!GlassPaneRenderer.active) {
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.GLASS_PANE_RENDERER_CLASS, "active", "Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderBlockPane));
        }

        private void setupBlockFace(final int face, final String direction) {
            faceMethods[face] = new MethodRef(getDeobfClass(), "render" + direction + "Face", "(LBlock;DDDI)V");

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override texture (" + direction.toLowerCase() + " face)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BinaryRegex.build(
                            // tessellator = Tessellator.instance;
                            reference(GETSTATIC, instance),
                            ASTORE, BinaryRegex.capture(BinaryRegex.any()),

                            // if (overrideBlockTexture >= 0) {
                            ALOAD_0,
                            reference(GETFIELD, overrideBlockTexture)
                        )),
                        IFLT, BinaryRegex.any(2),

                        // texture = overrideBlockTexture;
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_0,
                            reference(GETFIELD, overrideBlockTexture),
                            ISTORE, BinaryRegex.capture(BinaryRegex.any())
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // tessellator = Tessellator.instance;
                        // if (overrideBlockTexture >= 0) {
                        getCaptureGroup(1),
                        IFLT, branch("A"),

                        // texture = overrideBlockTexture;
                        getCaptureGroup(3),

                        // CTMUtils.reset();
                        reference(INVOKESTATIC, reset),
                        GOTO, branch("B"),

                        // } else if (CTMUtils.setup(this, block, (int) x, (int) y, (int) z, face, texture)) {
                        label("A"),
                        ALOAD_0,
                        ALOAD_1,
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        push(face),
                        ILOAD, getCaptureGroup(4),
                        reference(INVOKESTATIC, setup),
                        IFEQ, branch("B"),

                        // texture = CTMUtils.newTextureIndex;
                        reference(GETSTATIC, newTextureIndex),
                        ISTORE, getCaptureGroup(4),

                        // tessellator = CTMUtils.newTessellator;
                        reference(GETSTATIC, newTessellator),
                        ASTORE, getCaptureGroup(2),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(faceMethods[face]));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");
            final MethodRef start = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "start", "()V");
            final MethodRef finish = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "finish", "()V");

            classSignatures.add(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glNewList", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.000001F)
                    );
                }
            }.setMethod(updateRenderer));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "pre render world";
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
                        reference(INVOKESTATIC, start)
                    );
                }
            }.targetMethod(updateRenderer));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "post render world";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, finish),
                        RETURN
                    );
                }
            }.targetMethod(updateRenderer));
        }
    }
}
