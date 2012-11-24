package com.pclewis.mcpatcher;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class BaseTexturePackMod extends Mod {
    public static final String NAME = "__TexturePackBase";

    protected final MethodRef checkForTexturePackChange = new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS + "$ChangeHandler", "checkForTexturePackChange", "()V");
    protected final boolean haveFolderTexturePacks;
    protected final boolean haveITexturePack;
    protected final String texturePackType;

    protected BaseTexturePackMod(MinecraftVersion minecraftVersion) {
        name = NAME;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.1";

        haveFolderTexturePacks = minecraftVersion.compareTo("12w08a") >= 0;
        haveITexturePack = minecraftVersion.compareTo("12w15a") >= 0;
        texturePackType = haveITexturePack ? "LITexturePack;" : "LTexturePackBase;";

        addClassMod(new MinecraftMod());
        addClassMod(new RenderEngineMod());
        addClassMod(new TexturePackListMod());
        if (haveITexturePack) {
            addClassMod(new ITexturePackMod());
        }
        addClassMod(new TexturePackBaseMod());
        addClassMod(new TexturePackDefaultMod());
        addClassMod(new TexturePackCustomMod());
        if (haveFolderTexturePacks) {
            addClassMod(new TexturePackFolderMod());
        }
        addClassMod(new GetResourceMod());

        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS);
        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS + "$ChangeHandler");
        addClassFile(MCPatcherUtils.TEXTURE_PACK_API_CLASS + "$ChangeHandler$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS);
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$1");
        addClassFile(MCPatcherUtils.WEIGHTED_INDEX_CLASS + "$2");
    }

    @Override
    public String[] getLoggingCategories() {
        if (name.equals(NAME)) {
            return new String[]{"Texture Pack"};
        } else {
            return super.getLoggingCategories();
        }
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final FieldRef texturePackList = new FieldRef(getDeobfClass(), "texturePackList", "LTexturePackList;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
            final MethodRef startGame = new MethodRef(getDeobfClass(), "startGame", "()V");
            final MethodRef runGameLoop = new MethodRef(getDeobfClass(), "runGameLoop", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V"))
                    );
                }
            }.setMethod(startGame));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/opengl/Display", "isCloseRequested", "()Z"))
                    );
                }
            }.setMethod(runGameLoop));

            addMemberMapper(new FieldMapper(texturePackList));
            addMemberMapper(new FieldMapper(renderEngine));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "init texture pack handlers on startup";
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
                        reference(INVOKESTATIC, checkForTexturePackChange)
                    );
                }
            }.targetMethod(startGame));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check for texture pack change";
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
                        reference(INVOKESTATIC, checkForTexturePackChange)
                    );
                }
            }.targetMethod(runGameLoop));
        }
    }

    private class RenderEngineMod extends BaseMod.RenderEngineMod {
        RenderEngineMod() {
            final MethodRef deleteTexture = new MethodRef(getDeobfClass(), "deleteTexture", "(I)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteTextures", "(Ljava/nio/IntBuffer;)V"))
                    );
                }
            }.setMethod(deleteTexture));
        }
    }

    private class TexturePackListMod extends ClassMod {
        TexturePackListMod() {
            final FieldRef selectedTexturePack;
            final FieldRef defaultTexturePack;
            final MethodRef getDefaultTexturePack = new MethodRef(getDeobfClass(), "getDefaultTexturePack", "()LTexturePackBase;");
            final MethodRef getSelectedTexturePack = new MethodRef(getDeobfClass(), "getSelectedTexturePack", "()LTexturePackBase;");
            final MethodRef setTexturePack = new MethodRef(getDeobfClass(), "setTexturePack", "(" + texturePackType + ")Z");
            final MethodRef updateAvailableTexturePacks = new MethodRef(getDeobfClass(), "updateAvailableTexturePacks", "()V");
            final ClassRef texturePackClass = new ClassRef("TexturePackBase");

            addClassSignature(new ConstSignature(".zip"));
            addClassSignature(new ConstSignature("texturepacks"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/List", "removeAll", "(Ljava/util/Collection;)Z")),
                        POP
                    );
                }
            }.setMethod(updateAvailableTexturePacks));

            addMemberMapper(new MethodMapper(setTexturePack));

            if (haveITexturePack) {
                selectedTexturePack = new FieldRef(getDeobfClass(), "selectedTexturePack", "LITexturePack;");
                defaultTexturePack = new FieldRef(getDeobfClass(), "defaultTexturePack", "LITexturePack;");

                addMemberMapper(new FieldMapper(selectedTexturePack)
                    .accessFlag(AccessFlag.PRIVATE, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, false)
                );
                addMemberMapper(new FieldMapper(defaultTexturePack)
                    .accessFlag(AccessFlag.PRIVATE, true)
                    .accessFlag(AccessFlag.STATIC, true)
                    .accessFlag(AccessFlag.FINAL, true)
                );

                addPatch(new AddMethodPatch(getDefaultTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            reference(GETSTATIC, defaultTexturePack),
                            reference(CHECKCAST, texturePackClass),
                            ARETURN
                        );
                    }
                });

                addPatch(new AddMethodPatch(getSelectedTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, selectedTexturePack),
                            reference(CHECKCAST, texturePackClass),
                            ARETURN
                        );
                    }
                });
            } else {
                selectedTexturePack = new FieldRef(getDeobfClass(), "selectedTexturePack", "LTexturePackBase;");
                defaultTexturePack = new FieldRef(getDeobfClass(), "defaultTexturePack", "LTexturePackBase;");

                addMemberMapper(new FieldMapper(selectedTexturePack).accessFlag(AccessFlag.PUBLIC, true));
                addMemberMapper(new FieldMapper(defaultTexturePack).accessFlag(AccessFlag.PRIVATE, true));

                addPatch(new AddMethodPatch(getDefaultTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, defaultTexturePack),
                            ARETURN
                        );
                    }
                });

                addPatch(new AddMethodPatch(getSelectedTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, selectedTexturePack),
                            ARETURN
                        );
                    }
                });
            }

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "handle texture pack change";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1),
                        IRETURN
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, checkForTexturePackChange)
                    );
                }
            }.targetMethod(setTexturePack));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "handle texture pack list change";
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
                        reference(INVOKESTATIC, checkForTexturePackChange)
                    );
                }
            }.targetMethod(updateAvailableTexturePacks));
        }
    }

    private class ITexturePackMod extends ClassMod {
        ITexturePackMod() {
            prerequisiteClasses.add("TexturePackBase");

            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;")));
        }
    }

    private class TexturePackBaseMod extends ClassMod {
        TexturePackBaseMod() {
            final FieldRef file = new FieldRef(getDeobfClass(), "file", "Ljava/io/File;");
            final FieldRef texturePackFileName = new FieldRef(getDeobfClass(), "texturePackFileName", "Ljava/lang/String;");
            final MethodRef getInputStream = new MethodRef(getDeobfClass(), "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");

            addClassSignature(new ConstSignature(getResourceAsStream));

            if (haveITexturePack) {
                interfaces = new String[]{"ITexturePack"};

                addClassSignature(new ConstSignature("/pack.txt"));

                addMemberMapper(new FieldMapper(file));

                addPatch(new MakeMemberPublicPatch(file));
            } else {
                addClassSignature(new ConstSignature("pack.txt").negate(true));
            }

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, getResourceAsStream),
                        ARETURN
                    );
                }
            }.setMethod(getInputStream));

            addMemberMapper(new FieldMapper(texturePackFileName));

            addPatch(new MakeMemberPublicPatch(texturePackFileName));
        }
    }

    private class TexturePackDefaultMod extends ClassMod {
        TexturePackDefaultMod() {
            parentClass = "TexturePackBase";

            addClassSignature(new ConstSignature("The default look of Minecraft"));
        }
    }

    private class TexturePackCustomMod extends ClassMod {
        TexturePackCustomMod() {
            parentClass = "TexturePackBase";

            final FieldRef file = new FieldRef(getDeobfClass(), "file", "Ljava/io/File;");
            final FieldRef zipFile = new FieldRef(getDeobfClass(), "zipFile", "Ljava/util/zip/ZipFile;");

            addClassSignature(new ConstSignature(new MethodRef("java/util/zip/ZipFile", "getEntry", "(Ljava/lang/String;)Ljava/util/zip/ZipEntry;")));
            addClassSignature(new ConstSignature(new MethodRef("java/util/zip/ZipFile", "close", "()V")));

            if (!haveITexturePack) {
                addClassSignature(new ConstSignature("pack.txt"));
                addClassSignature(new ConstSignature("pack.png"));

                addMemberMapper(new FieldMapper(file));

                addPatch(new MakeMemberPublicPatch(file));
            }

            addMemberMapper(new FieldMapper(zipFile));

            addPatch(new MakeMemberPublicPatch(zipFile));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "origZip", "Ljava/util/zip/ZipFile;")));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "tmpFile", "Ljava/io/File;")));
            addPatch(new AddFieldPatch(new FieldRef(getDeobfClass(), "lastModified", "J")));
        }
    }

    private class TexturePackFolderMod extends ClassMod {
        TexturePackFolderMod() {
            parentClass = "TexturePackBase";

            final String fileFieldName = haveITexturePack ? "file" : "folder";
            final FieldRef file = new FieldRef(getDeobfClass(), fileFieldName, "Ljava/io/File;");
            final MethodRef getFolder = new MethodRef(getDeobfClass(), "getFolder", "()Ljava/io/File;");
            final MethodRef substring = new MethodRef("java/lang/String", "substring", "(I)Ljava/lang/String;");

            addClassSignature(new ConstSignature(new ClassRef("java.io.FileInputStream")));

            if (haveITexturePack) {
                addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_1,
                            push(1),
                            reference(INVOKEVIRTUAL, substring)
                        );
                    }
                });
            } else {
                addClassSignature(new ConstSignature("pack.txt"));
                addClassSignature(new ConstSignature("pack.png"));

                addMemberMapper(new FieldMapper(file));
            }

            addPatch(new AddMethodPatch(getFolder) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, file),
                        ARETURN
                    );
                }
            });
        }
    }

    private class GetResourceMod extends ClassMod {
        GetResourceMod() {
            global = true;

            final MethodRef getResource = new MethodRef("java.lang.Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;");
            final MethodRef readURL = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/net/URL;)Ljava/awt/image/BufferedImage;");
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
            final MethodRef readStream = new MethodRef("javax.imageio.ImageIO", "read", "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;");

            addClassSignature(new OrSignature(
                new ConstSignature(getResource),
                new ConstSignature(getResourceAsStream)
            ));
            addClassSignature(new OrSignature(
                new ConstSignature(readURL),
                new ConstSignature(readStream)
            ));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "ImageIO.read(getResource(...)) -> getImage(...)";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.TEXTURE_PACK_API_CLASS, "getImage", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/awt/image/BufferedImage;"))
                    );
                }
            });
        }
    }
}
