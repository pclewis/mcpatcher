package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
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
        version = "1.2";
        defaultEnabled = false;

        configPanel = new ConfigPanel();

        classMods.add(new MinecraftMod());
        classMods.add(new RenderEngineMod());
        classMods.add(new BaseMod.TexturePackListMod(minecraftVersion));
        classMods.add(new BaseMod.TexturePackBaseMod(minecraftVersion));
        classMods.add(new BaseMod.IBlockAccessMod());
        classMods.add(new BlockMod());
        classMods.add(new TessellatorMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new WorldRendererMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CTM_UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.SUPER_TESSELLATOR_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$CTM"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Random1"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Horizontal"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Top"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.TILE_OVERRIDE_CLASS + "$Repeat"));
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

            glassPaneCheckBox.setVisible(false);
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
            mapTexturePackList();

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;")));
        }
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            classSignatures.add(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "allocateAndSetupTexture", "(Ljava/awt/image/BufferedImage;)I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            final MethodRef getBlockTexture = new MethodRef(getDeobfClass(), "getBlockTexture", "(LIBlockAccess;IIII)I");

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
                .addXref(1, new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I"))
                .addXref(2, new MethodRef(getDeobfClass(), "getBlockTextureFromSideAndMetadata", "(II)I"))
            );
        }
    }

    private class TessellatorMod extends ClassMod {
        TessellatorMod() {
            final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
            final MethodRef startDrawingQuads = new MethodRef(getDeobfClass(), "startDrawingQuads", "()V");
            final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(I)V");
            final MethodRef constructor = new MethodRef("Tessellator", "<init>", "(I)V");
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final MethodRef addVertex = new MethodRef(getDeobfClass(), "addVertex", "(DDD)V");
            final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellator;");
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
                        push("Not tesselating!")
                    );
                }
            }.setMethod(draw));

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
                            BinaryRegex.capture(BytecodeMatcher.anyLDC),
                            reference(INVOKESPECIAL, constructor)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(NEW, new ClassRef(MCPatcherUtils.SUPER_TESSELLATOR_CLASS)),
                        DUP,
                        getCaptureGroup(1),
                        reference(INVOKESPECIAL, new MethodRef(MCPatcherUtils.SUPER_TESSELLATOR_CLASS, "<init>", "(I)V"))
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
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
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

        @Override
        public void prePatch(String filename, ClassFile classFile) {
            getClassMap().addInheritance(getDeobfClass(), MCPatcherUtils.SUPER_TESSELLATOR_CLASS);
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];
        private final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "I");
        private final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        private final FieldRef instance = new FieldRef("Tessellator", "instance", "LTessellator;");
        private final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;III)Z");
        private final MethodRef drawCrossedSquares = new MethodRef(getDeobfClass(), "drawCrossedSquares", "(LBlock;IDDD)V");
        private final MethodRef setup = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "setup", "(LBlock;LIBlockAccess;IIIII)Z");
        private final MethodRef setupNoFace = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "setup", "(LBlock;LIBlockAccess;IIII)Z");
        private final MethodRef reset = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "reset", "()V");
        private final FieldRef newTextureIndex = new FieldRef(MCPatcherUtils.CTM_UTILS_CLASS, "newTextureIndex", "I");
        private final FieldRef newTessellator = new FieldRef(MCPatcherUtils.CTM_UTILS_CLASS, "newTessellator", "LTessellator;");

        private ArrayList<MethodInfo> renderMethods = new ArrayList<MethodInfo>();
        private ArrayList<Integer> tessellatorRegisters = new ArrayList<Integer>();

        RenderBlocksMod() {
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

            memberMappers.add(new FieldMapper(blockAccess));
            memberMappers.add(new MethodMapper(faceMethods));
            memberMappers.add(new MethodMapper(drawCrossedSquares));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "find potential render methods";
                }

                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    return methodInfo.getDescriptor().matches("^\\(L[a-z]+;III.*");
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

                        // && CTMUtils.setup(block, blockAccess, i, j, k, texture)) {
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
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

                        // && CTMUtils.setup(block, blockAccess, (int) x, (int) y, (int) z, texture)) {
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
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
                            reference(GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
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

                        // } else if (CTMUtils.setup(block, blockAccess, (int) x, (int) y, (int) z, face, texture)) {
                        label("A"),
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
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
