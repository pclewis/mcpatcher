package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TexturePackBase;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;

public class CTMUtils {
    private static final boolean enableGlass = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glass", true);
    private static final boolean enableGlassPane = false;
    private static final boolean enableBookshelf = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", true);
    private static final boolean enableSandstone = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", true);
    private static final boolean enableOther = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "other", true);

    private static final int BLOCK_ID_GLASS = 20;
    private static final int BLOCK_ID_GLASS_PANE = 102;
    private static final int BLOCK_ID_BOOKSHELF = 47;
    private static final int BLOCK_ID_SANDSTONE = 24;

    private static final int BOTTOM_FACE = 0; // 0, -1, 0
    private static final int TOP_FACE = 1; // 0, 1, 0
    private static final int NORTH_FACE = 2; // 0, 0, -1
    private static final int SOUTH_FACE = 3; // 0, 0, 1
    private static final int WEST_FACE = 4; // -1, 0, 0
    private static final int EAST_FACE = 5; // 1, 0, 0

    private static final int[] GO_DOWN = new int[]{0, -1, 0};
    private static final int[] GO_UP = new int[]{0, 1, 0};
    private static final int[] GO_NORTH = new int[]{0, 0, -1};
    private static final int[] GO_SOUTH = new int[]{0, 0, 1};
    private static final int[] GO_WEST = new int[]{-1, 0, 0};
    private static final int[] GO_EAST = new int[]{1, 0, 0};

    // NEIGHBOR_OFFSETS[a][b][c] = offset from starting block
    // a: face 0-5
    // b: neighbor 0-7
    // c: coordinate (x,y,z) 0-2
    private static final int[][][] NEIGHBOR_OFFSET = new int[][][]{
        // BOTTOM_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_SOUTH),
            GO_SOUTH,
            add(GO_EAST, GO_SOUTH),
            GO_EAST,
            add(GO_EAST, GO_NORTH),
            GO_NORTH,
            add(GO_WEST, GO_NORTH),
        },
        // TOP_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_SOUTH),
            GO_SOUTH,
            add(GO_EAST, GO_SOUTH),
            GO_EAST,
            add(GO_EAST, GO_NORTH),
            GO_NORTH,
            add(GO_WEST, GO_NORTH),
        },
        // NORTH_FACE
        {
            GO_EAST,
            add(GO_EAST, GO_DOWN),
            GO_DOWN,
            add(GO_WEST, GO_DOWN),
            GO_WEST,
            add(GO_WEST, GO_UP),
            GO_UP,
            add(GO_EAST, GO_UP),
        },
        // SOUTH_FACE
        {
            GO_WEST,
            add(GO_WEST, GO_DOWN),
            GO_DOWN,
            add(GO_EAST, GO_DOWN),
            GO_EAST,
            add(GO_EAST, GO_UP),
            GO_UP,
            add(GO_WEST, GO_UP),
        },
        // WEST_FACE
        {
            GO_NORTH,
            add(GO_NORTH, GO_DOWN),
            GO_DOWN,
            add(GO_SOUTH, GO_DOWN),
            GO_SOUTH,
            add(GO_SOUTH, GO_UP),
            GO_UP,
            add(GO_NORTH, GO_UP),
        },
        // EAST_FACE
        {
            GO_SOUTH,
            add(GO_SOUTH, GO_DOWN),
            GO_DOWN,
            add(GO_NORTH, GO_DOWN),
            GO_NORTH,
            add(GO_NORTH, GO_UP),
            GO_UP,
            add(GO_SOUTH, GO_UP),
        },
    };

    // Index into this array is formed from these bit values:
    // 128 64  32
    // 1   *   16
    // 2   4   8
    private static final int[] GLASS_TEXTURE_INDEX = new int[]{
        0, 3, 0, 3, 16, 5, 16, 19, 0, 3, 0, 3, 16, 5, 16, 19,
        1, 2, 1, 2, 4, 7, 4, 37, 1, 2, 1, 2, 17, 39, 17, 18,
        0, 3, 0, 3, 16, 5, 16, 19, 0, 3, 0, 3, 16, 5, 16, 19,
        1, 2, 1, 2, 4, 7, 4, 37, 1, 2, 1, 2, 17, 39, 17, 18,
        48, 21, 48, 21, 32, 23, 32, 55, 48, 21, 48, 21, 32, 23, 32, 55,
        20, 22, 20, 22, 6, 58, 6, 25, 20, 22, 20, 22, 36, 9, 36, 26,
        48, 21, 48, 21, 32, 23, 32, 55, 48, 21, 48, 21, 32, 23, 32, 55,
        49, 52, 49, 52, 38, 8, 38, 42, 49, 52, 49, 52, 33, 27, 33, 57,
        0, 3, 0, 3, 16, 5, 16, 19, 0, 3, 0, 3, 16, 5, 16, 19,
        1, 2, 1, 2, 4, 7, 4, 37, 1, 2, 1, 2, 17, 39, 17, 18,
        0, 3, 0, 3, 16, 5, 16, 19, 0, 3, 0, 3, 16, 5, 16, 19,
        1, 2, 1, 2, 4, 7, 4, 37, 1, 2, 1, 2, 17, 39, 17, 18,
        48, 51, 48, 51, 32, 53, 32, 35, 48, 51, 48, 51, 32, 53, 32, 35,
        20, 54, 20, 54, 6, 24, 6, 10, 20, 54, 20, 54, 36, 43, 36, 56,
        48, 51, 48, 51, 32, 53, 32, 35, 48, 51, 48, 51, 32, 53, 32, 35,
        49, 50, 49, 50, 38, 11, 38, 40, 49, 50, 49, 50, 33, 41, 33, 34,
    };

    // Index into this array is formed from these bit values:
    // 1   *   2
    private static final int[] BOOKSHELF_TEXTURE_INDEX = new int[]{
        15, 14, 12, 13,
    };

    private static TexturePackBase lastTexturePack;
    private static final int availTexture[] = new int[Block.blocksList.length];
    private static int terrainTexture;
    private static int newTexture;
    private static int newTextureIndex;
    private static boolean textureChanged;
    private static boolean active;

    public static void start() {
        checkUpdate();
        active = true;
    }

    public static int setup(Block block, IBlockAccess blockAccess, int i, int j, int k, int face, int origTexture) {
        if (!active || blockAccess == null || face < 0 || face > 5) {
            return origTexture;
        }
        if (getConnectedTexture(blockAccess, block.blockID, i, j, k, face) && bindTexture(newTexture)) {
            textureChanged = true;
            return newTextureIndex;
        } else {
            reset();
            return origTexture;
        }
    }

    public static void reset() {
        if (textureChanged) {
            bindTexture();
            textureChanged = false;
        }
    }

    public static boolean finish(boolean b) {
        reset();
        active = false;
        return b;
    }

    private static boolean getConnectedTexture(IBlockAccess blockAccess, int blockId, int i, int j, int k, int face) {
        if (blockId < 0 || blockId >= availTexture.length) {
            return false;
        }
        newTextureIndex = 0;
        newTexture = availTexture[blockId];
        if (newTexture < 0) {
            return false;
        }

        int[][] offsets = NEIGHBOR_OFFSET[face];
        int neighborBits = 0;
        switch (blockId) {
            default:
            case BLOCK_ID_GLASS:
            case BLOCK_ID_GLASS_PANE:
                for (int bit = 0; bit < 8; bit++) {
                    if (shouldConnect(blockAccess, blockId, i, j, k, offsets[bit])) {
                        neighborBits |= (1 << bit);
                    }
                }
                newTextureIndex = GLASS_TEXTURE_INDEX[neighborBits];
                return true;

            case BLOCK_ID_BOOKSHELF:
                if (face < 2) {
                    return false;
                }
                for (int bit = 0; bit < 2; bit++) {
                    if (shouldConnect(blockAccess, blockId, i, j, k, offsets[4 * bit])) {
                        neighborBits |= (1 << bit);
                    }
                }
                newTextureIndex = BOOKSHELF_TEXTURE_INDEX[neighborBits];
                return true;

            case BLOCK_ID_SANDSTONE:
                if (face < 2) {
                    return false;
                }
                if (blockAccess.getBlockMetadata(i, j, k) != 0) {
                    return false;
                }
                if (shouldConnect(blockAccess, blockId, i, j, k, GO_UP)) {
                    newTextureIndex = 66;
                    return true;
                }
                return false;
        }
    }

    private static void checkUpdate() {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.getSelectedTexturePack();
        if (selectedTexturePack == lastTexturePack) {
            return;
        }
        MCPatcherUtils.info("refreshing connected textures");
        lastTexturePack = selectedTexturePack;
        terrainTexture = getTexture("/terrain.png");
        for (int i = 0; i < availTexture.length; i++) {
            String textureName = null;
            switch (i) {
                case BLOCK_ID_GLASS:
                    if (enableGlass) {
                        textureName = "/ctm.png";
                    }
                    break;

                case BLOCK_ID_GLASS_PANE:
                    if (enableGlassPane) {
                        textureName = "/ctm.png";
                    }
                    break;

                case BLOCK_ID_BOOKSHELF:
                    if (enableBookshelf) {
                        textureName = "/ctm.png";
                    }
                    break;

                case BLOCK_ID_SANDSTONE:
                    if (enableSandstone) {
                        textureName = "/ctm.png";
                    }
                    break;

                default:
                    if (enableOther) {
                        textureName = "/ctm/block" + i + ".png";
                    }
                    break;
            }
            availTexture[i] = getTexture(textureName);
            if (availTexture[i] >= 0) {
                MCPatcherUtils.info("using %s (texture id %d) for block %d", textureName, availTexture[i], i);
            }
        }
        bindTexture();
    }

    private static int getTexture(String name) {
        if (name == null) {
            return -1;
        }
        BufferedImage image = MCPatcherUtils.readImage(lastTexturePack.getInputStream(name));
        if (image == null) {
            return -1;
        } else {
            return MCPatcherUtils.getMinecraft().renderEngine.getTexture(name);
        }
    }

    private static boolean bindTexture(int texture) {
        if (texture >= 0) {
            int curTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            if (curTexture != texture) {
                Tessellator tessellator = Tessellator.instance;
                tessellator.preserve = true;
                tessellator.draw();
                tessellator.startDrawingQuads();
                tessellator.preserve = false;
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean bindTexture() {
        return bindTexture(terrainTexture);
    }

    private static boolean shouldConnect(IBlockAccess blockAccess, int blockId, int i, int j, int k, int[] offset) {
        return blockAccess.getBlockId(i + offset[0], j + offset[1], k + offset[2]) == blockId;
    }

    private static int[] add(int[] a, int[] b) {
        if (a.length != b.length) {
            throw new RuntimeException("arrays to add are not same length");
        }
        int[] c = new int[a.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
    }
}
