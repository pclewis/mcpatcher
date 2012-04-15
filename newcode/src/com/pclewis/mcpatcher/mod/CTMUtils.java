package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TexturePackBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CTMUtils {
    private static final boolean enableGlass = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glass", true);
    private static final boolean enableGlassPane = false;
    private static final boolean enableBookshelf = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", true);
    private static final boolean enableSandstone = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", true);
    private static final boolean enableOther = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "other", true);
    private static final boolean enableOutline = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "outline", false);

    private static final int NUM_TILES = 256;

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
    private static final int[] GENERIC_TEXTURE_INDEX = new int[]{
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
    private static TextureOverride blocks[];
    private static TextureOverride tiles[];
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
        if (getConnectedTexture(blockAccess, block.blockID, origTexture, i, j, k, face) && bindTexture(newTexture)) {
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

    private static boolean getConnectedTexture(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
        return getConnectedTextureByBlock(blockAccess, blockId, origTexture, i, j, k, face) ||
            getConnectedTextureByTile(blockAccess, blockId, origTexture, i, j, k, face);
    }

    private static boolean getConnectedTextureByBlock(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
        if (blockId < 0 || blockId >= blocks.length) {
            return false;
        }
        TextureOverride override = blocks[blockId];
        if (override == null || (override.faces & (1 << face)) == 0) {
            return false;
        }
        newTextureIndex = 0;
        newTexture = override.texture;
        if (newTexture < 0) {
            return false;
        }

        switch (blockId) {
            default:
            case BLOCK_ID_GLASS:
            case BLOCK_ID_GLASS_PANE:
                return override.override(blockAccess, blockId, origTexture, i, j, k, face);

            case BLOCK_ID_BOOKSHELF:
                return override.overrideBookshelf(blockAccess, blockId, origTexture, i, j, k, face);

            case BLOCK_ID_SANDSTONE:
                return override.overrideSandstone(blockAccess, blockId, origTexture, i, j, k, face);
        }
    }

    private static boolean getConnectedTextureByTile(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
        if (origTexture < 0 || origTexture >= tiles.length) {
            return false;
        }
        TextureOverride override = tiles[origTexture];
        if (override == null || (override.faces & (1 << face)) == 0) {
            return false;
        }
        newTextureIndex = 0;
        newTexture = override.texture;
        if (newTexture < 0) {
            return false;
        }

        return override.override(blockAccess, blockId, origTexture, i, j, k, face);
    }

    private static void checkUpdate() {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.getSelectedTexturePack();
        if (selectedTexturePack == lastTexturePack) {
            return;
        }
        MCPatcherUtils.info("refreshing connected textures");
        lastTexturePack = selectedTexturePack;
        terrainTexture = getTexture("/terrain.png");

        refreshBlockTextures();
        refreshTileTextures();

        bindTexture();
    }

    private static void refreshBlockTextures() {
        blocks = new TextureOverride[Block.blocksList.length];
        for (int i = 0; i < blocks.length; i++) {
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
            if (textureName != null) {
                TextureOverride override = new TextureOverride("block", textureName);
                if (override.isValid()) {
                    MCPatcherUtils.info("using %s (texture id %d) for block %d", textureName, override.texture, i);
                    blocks[i] = override;
                }
            }
        }
    }

    private static void refreshTileTextures() {
        tiles = new TextureOverride[NUM_TILES];
        if (enableOther) {
            for (int i = 0; i < tiles.length; i++) {
                TextureOverride override = new TextureOverride("terrain", i);
                if (override.isValid()) {
                    MCPatcherUtils.info("using %s (texture id %d) for terrain tile %d", override.textureName, override.texture, i);
                }
            }
        }
        if (enableOutline) {
            setupOutline();
        }
    }

    private static void setupOutline() {
        BufferedImage terrain = MCPatcherUtils.readImage(lastTexturePack.getInputStream("/terrain.png"));
        if (terrain == null) {
            return;
        }
        BufferedImage template = MCPatcherUtils.readImage(lastTexturePack.getInputStream("/ctm/template.png"));
        if (template == null) {
            return;
        }

        int width = terrain.getWidth();
        int height = terrain.getHeight();
        if (template.getWidth() != width) {
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = newImage.createGraphics();
            graphics2D.drawImage(template, 0, 0, width, height, null);
            template = newImage;
        }

        for (int i = 0; i < tiles.length; i++) {
            setupOutline(i, terrain, template);
        }
    }

    private static void setupOutline(int tileNum, BufferedImage terrain, BufferedImage template) {
        if (tiles[tileNum] != null) {
            return;
        }
        switch (tileNum) {
            case 14 * 16 + 13: // still lava
            case 14 * 16 + 14: // flowing lava
            case 12 * 16 + 13: // still water
            case 12 * 16 + 14: // flowing water
            case 1 * 16 + 15: // fire east-west
            case 2 * 16 + 15: // fire north-south
            case 0 * 16 + 14: // portal
                return;

            default:
                break;
        }

        int tileSize = terrain.getWidth() / 16;
        int tileX = (tileNum % 16) * tileSize;
        int tileY = (tileNum / 16) * tileSize;
        BufferedImage newImage = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < template.getWidth(); x++) {
            for (int y = 0; y < template.getHeight(); y++) {
                int rgb = template.getRGB(x, y);
                if ((rgb & 0xff000000) == 0) {
                    rgb = terrain.getRGB(tileX + (x % tileSize), tileY + (y % tileSize));
                }
                newImage.setRGB(x, y, rgb);
            }
        }

        TextureOverride override = new TextureOverride("tile", newImage);
        if (override.isValid()) {
            tiles[tileNum] = override;
        }
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

    private static class TextureOverride {
        private static final long MULTIPLIER = 0x5deece66dL;
        private static final long ADDEND = 0xbL;
        private static final long MASK = (1L << 48) - 1;

        final String type;
        final String textureName;
        final int texture;
        final int faces;
        final boolean random;
        final int numTiles;

        TextureOverride(String type, int index) {
            this(type, "/ctm/" + type + index + ".png");
        }

        TextureOverride(String type, String textureName) {
            this.type = type;
            this.textureName = textureName;

            texture = getTexture(textureName);

            String filename = textureName.replaceFirst("\\.png$", ".properties");
            InputStream is = null;
            Properties properties = new Properties();
            try {
                is = lastTexturePack.getInputStream(filename);
                if (is != null) {
                    properties.load(is);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(is);
            }

            int flags = 0;
            for (String val : properties.getProperty("faces", "all").trim().toLowerCase().split("\\s+")) {
                if (val.equals("bottom")) {
                    flags |= (1 << BOTTOM_FACE);
                } else if (val.equals("top")) {
                    flags |= (1 << TOP_FACE);
                } else if (val.equals("north")) {
                    flags |= (1 << NORTH_FACE);
                } else if (val.equals("south")) {
                    flags |= (1 << SOUTH_FACE);
                } else if (val.equals("east")) {
                    flags |= (1 << EAST_FACE);
                } else if (val.equals("west")) {
                    flags |= (1 << WEST_FACE);
                } else if (val.equals("side") || val.equals("sides")) {
                    flags |= (1 << NORTH_FACE) | (1 << SOUTH_FACE) | (1 << EAST_FACE) | (1 << WEST_FACE);
                } else if (val.equals("all")) {
                    flags = -1;
                }
            }
            faces = flags;

            String val = properties.getProperty("mode", "ctm").trim().toLowerCase();
            if (val.equals("random")) {
                val = properties.getProperty("tiles", "0").trim();
                int n = 0;
                try {
                    n = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                }
                random = true;
                numTiles = n;
            } else {
                random = false;
                numTiles = 0;
            }
        }

        TextureOverride(String type, BufferedImage image) {
            this.type = type;
            textureName = null;
            texture = MCPatcherUtils.getMinecraft().renderEngine.allocateAndSetupTexture(image);
            faces = -1;
            random = false;
            numTiles = 0;
        }

        boolean isValid() {
            if (texture < 0) {
                return false;
            }
            if (random && numTiles <= 0) {
                return false;
            }
            return true;
        }

        boolean override(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
            if (random) {
                long n = face;
                n <<= 16;
                n ^= i;
                n <<= 16;
                n ^= j;
                n <<= 16;
                n ^= k;
                n = MULTIPLIER * n + ADDEND;
                n = MULTIPLIER * n + ADDEND;
                n &= MASK;
                newTextureIndex = (int) (((double) n / (double) (MASK + 1)) * numTiles);
            } else {
                int[][] offsets = NEIGHBOR_OFFSET[face];
                int neighborBits = 0;
                for (int bit = 0; bit < 8; bit++) {
                    if (shouldConnect(blockAccess, blockId, i, j, k, offsets[bit])) {
                        neighborBits |= (1 << bit);
                    }
                }
                newTextureIndex = GENERIC_TEXTURE_INDEX[neighborBits];
            }
            return true;
        }

        boolean overrideBookshelf(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
            if (face < 2) {
                return false;
            }
            int neighborBits = 0;
            int[][] offsets = NEIGHBOR_OFFSET[face];
            for (int bit = 0; bit < 2; bit++) {
                if (shouldConnect(blockAccess, blockId, i, j, k, offsets[4 * bit])) {
                    neighborBits |= (1 << bit);
                }
            }
            newTextureIndex = BOOKSHELF_TEXTURE_INDEX[neighborBits];
            return true;
        }

        boolean overrideSandstone(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
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
}
