package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TexturePackBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Properties;

public class CTMUtils {
    private static final boolean enableGlass = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glass", true);
    private static final boolean enableGlassPane = false;
    private static final boolean enableBookshelf = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", true);
    private static final boolean enableSandstone = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", true);
    private static final boolean enableStandard = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true);
    private static final boolean enableNonStandard = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true);
    private static final boolean enableOutline = MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "outline", false);

    static final int BLOCK_ID_GLASS = 20;
    static final int BLOCK_ID_BED = 26;
    static final int BLOCK_ID_GLASS_PANE = 102;
    static final int BLOCK_ID_BOOKSHELF = 47;

    static final int TERRAIN_ID_SANDSTONE_SIDE = 192;

    static final int NUM_TILES = 256;

    static final int TILE_NUM_STILL_LAVA = 14 * 16 + 13;
    static final int TILE_NUM_FLOWING_LAVA = 14 * 16 + 14;
    static final int TILE_NUM_STILL_WATER = 12 * 16 + 13;
    static final int TILE_NUM_FLOWING_WATER = 12 * 16 + 14;
    static final int TILE_NUM_FIRE_E_W = 1 * 16 + 15;
    static final int TILE_NUM_FIRE_N_S = 2 * 16 + 15;
    static final int TILE_NUM_PORTAL = 0 * 16 + 14;

    static final int BOTTOM_FACE = 0; // 0, -1, 0
    static final int TOP_FACE = 1; // 0, 1, 0
    static final int NORTH_FACE = 2; // 0, 0, -1
    static final int SOUTH_FACE = 3; // 0, 0, 1
    static final int WEST_FACE = 4; // -1, 0, 0
    static final int EAST_FACE = 5; // 1, 0, 0

    static final int[] GO_DOWN = new int[]{0, -1, 0};
    static final int[] GO_UP = new int[]{0, 1, 0};
    static final int[] GO_NORTH = new int[]{0, 0, -1};
    static final int[] GO_SOUTH = new int[]{0, 0, 1};
    static final int[] GO_WEST = new int[]{-1, 0, 0};
    static final int[] GO_EAST = new int[]{1, 0, 0};

    // NEIGHBOR_OFFSETS[a][b][c] = offset from starting block
    // a: face 0-5
    // b: neighbor 0-7
    // c: coordinate (x,y,z) 0-2
    static final int[][][] NEIGHBOR_OFFSET = new int[][][]{
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

    static TexturePackBase lastTexturePack;
    static int terrainTexture;
    private static TileOverride blockOverrides[];
    private static TileOverride tileOverrides[];

    private static boolean active;
    private static int newTexture;

    public static int newTextureIndex;
    public static Tessellator newTessellator;

    public static void start() {
        checkUpdate();
        Tessellator.instance.texture = terrainTexture;
        if (Tessellator.instance instanceof SuperTessellator) {
            active = true;
        }
    }

    public static boolean setup(Block block, IBlockAccess blockAccess, int i, int j, int k, int face, int origTexture) {
        if (!active || !enableStandard || blockAccess == null || face < 0 || face > 5) {
            return false;
        }
        if (block.blockID == BLOCK_ID_BED) {
            return false;
        }
        if (getConnectedTexture(blockAccess, block, origTexture, i, j, k, face)) {
            newTessellator = ((SuperTessellator) Tessellator.instance).getTessellator(newTexture);
            return true;
        } else {
            reset();
            return false;
        }
    }

    public static boolean setup(Block block, IBlockAccess blockAccess, int i, int j, int k, int origTexture) {
        if (!active || !enableNonStandard || blockAccess == null) {
            return false;
        }
        if (block.blockID == BLOCK_ID_BED) {
            return false;
        }
        if (getConnectedTexture(blockAccess, block, origTexture, i, j, k, -1)) {
            newTessellator = ((SuperTessellator) Tessellator.instance).getTessellator(newTexture);
            return true;
        } else {
            reset();
            return false;
        }
    }

    public static void reset() {
    }

    public static void finish() {
        reset();
        Tessellator.instance.texture = -1;
        active = false;
    }

    private static boolean getConnectedTexture(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
        return getConnectedTexture(blockAccess, block, origTexture, i, j, k, face, tileOverrides, origTexture) ||
            getConnectedTexture(blockAccess, block, origTexture, i, j, k, face, blockOverrides, block.blockID);
    }

    private static boolean getConnectedTexture(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face, TileOverride[] overrides, int index) {
        if (index < 0 || index >= overrides.length) {
            return false;
        }
        TileOverride override = overrides[index];
        if (override == null) {
            return false;
        }
        if (override.disabled) {
            overrides[index] = null;
            return false;
        }
        newTexture = override.texture;
        newTextureIndex = override.getTile(blockAccess, block, origTexture, i, j, k, face);
        return newTextureIndex >= 0;
    }

    private static void checkUpdate() {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.getSelectedTexturePack();
        if (selectedTexturePack == lastTexturePack) {
            return;
        }
        MCPatcherUtils.info("refreshing connected textures");
        lastTexturePack = selectedTexturePack;
        terrainTexture = getTexture("/terrain.png");
        if (Tessellator.instance instanceof SuperTessellator) {
            ((SuperTessellator) Tessellator.instance).clearTessellators();
        }

        refreshBlockTextures();
        refreshTileTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, terrainTexture);
    }

    private static void refreshBlockTextures() {
        blockOverrides = new TileOverride[Block.blocksList.length];
        for (int i = 0; i < blockOverrides.length; i++) {
            String prefix = null;
            TileOverride override = null;
            if (enableStandard || enableNonStandard) {
                prefix = "/ctm/block" + i;
                override = TileOverride.create(prefix, null, false);
            }
            if (override == null) {
                Properties properties = new Properties();
                switch (i) {
                    case BLOCK_ID_GLASS:
                        if (enableGlass) {
                            prefix = "/ctm";
                            properties.setProperty("method", "glass");
                            properties.setProperty("connect", "block");
                        }
                        break;

                    case BLOCK_ID_GLASS_PANE:
                        if (enableGlassPane) {
                            prefix = "/ctm";
                            properties.setProperty("method", "glass");
                            properties.setProperty("connect", "block");
                        }
                        break;

                    case BLOCK_ID_BOOKSHELF:
                        if (enableBookshelf) {
                            prefix = "/ctm";
                            properties.setProperty("method", "bookshelf");
                            properties.setProperty("connect", "block");
                        }
                        break;

                    default:
                        continue;
                }
                override = TileOverride.create(prefix, properties, false);
            }
            if (override != null) {
                MCPatcherUtils.info("using %s (texture id %d) for block %d", override.textureName, override.texture, i);
                blockOverrides[i] = override;
            }
        }
    }

    private static void refreshTileTextures() {
        tileOverrides = new TileOverride[NUM_TILES];
        if (enableStandard || enableNonStandard) {
            for (int i = 0; i < tileOverrides.length; i++) {
                TileOverride override = TileOverride.create("/ctm/terrain" + i, null, true);
                if (override == null) {
                    Properties properties = new Properties();
                    switch (i)
                    {
                        case TERRAIN_ID_SANDSTONE_SIDE:
                            properties.setProperty("method", "sandstone");
                            properties.setProperty("connect", "tile");
                            break;

                        default:
                            continue;
                    }
                    override = TileOverride.create("/ctm", properties, true);
                }
                if (override != null) {
                    MCPatcherUtils.info("using %s (texture id %d) for terrain tile %d", override.textureName, override.texture, i);
                    tileOverrides[i] = override;
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

        for (int i = 0; i < tileOverrides.length; i++) {
            setupOutline(i, terrain, template);
        }
    }

    private static void setupOutline(int tileNum, BufferedImage terrain, BufferedImage template) {
        if (tileOverrides[tileNum] != null) {
            return;
        }
        switch (tileNum) {
            case TILE_NUM_STILL_LAVA: // still lava
            case TILE_NUM_FLOWING_LAVA: // flowing lava
            case TILE_NUM_STILL_WATER: // still water
            case TILE_NUM_FLOWING_WATER: // flowing water
            case TILE_NUM_FIRE_E_W: // fire east-west
            case TILE_NUM_FIRE_N_S: // fire north-south
            case TILE_NUM_PORTAL: // portal
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

        tileOverrides[tileNum] = TileOverride.create(newImage);
    }

    static int getTexture(String name) {
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
