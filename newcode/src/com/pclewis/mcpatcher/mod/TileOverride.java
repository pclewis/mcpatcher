package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.IBlockAccess;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

abstract class TileOverride {
    final String textureName;
    final int texture;
    final int faces;
    final int[] tileMap;

    static TileOverride create(String filePrefix) {
        InputStream is = null;
        Properties properties = new Properties();
        try {
            is = CTMUtils.lastTexturePack.getInputStream(filePrefix + ".properties");
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(is);
        }
        return create(filePrefix, properties);
    }

    static TileOverride create(String filePrefix, Properties properties) {
        String method = properties.getProperty("method", "default").trim().toLowerCase();
        TileOverride override = null;

        if (method.equals("default") || method.equals("glass") || method.equals("ctm")) {
            override = new Default(filePrefix, properties);
        } else if (method.equals("bookshelf") || method.equals("horizontal")) {
            override = new Horizontal(filePrefix, properties);
        } else if (method.equals("sandstone") || method.equals("top")) {
            override = new Top(filePrefix, properties);
        } else {
            MCPatcherUtils.error("unknown method \"%s\" in %s.properties", method, filePrefix);
        }

        if (override != null && override.isValid()) {
            return override;
        } else {
            return null;
        }
    }

    TileOverride(String filePrefix, Properties properties) {
        textureName = properties.getProperty("source", filePrefix + ".png");
        texture = CTMUtils.getTexture(textureName);
        if (texture < 0) {
            MCPatcherUtils.error("source texture %s not found", textureName);
        }

        int flags = 0;
        for (String val : properties.getProperty("faces", "all").trim().toLowerCase().split("\\s+")) {
            if (val.equals("bottom")) {
                flags |= (1 << CTMUtils.BOTTOM_FACE);
            } else if (val.equals("top")) {
                flags |= (1 << CTMUtils.TOP_FACE);
            } else if (val.equals("north")) {
                flags |= (1 << CTMUtils.NORTH_FACE);
            } else if (val.equals("south")) {
                flags |= (1 << CTMUtils.SOUTH_FACE);
            } else if (val.equals("east")) {
                flags |= (1 << CTMUtils.EAST_FACE);
            } else if (val.equals("west")) {
                flags |= (1 << CTMUtils.WEST_FACE);
            } else if (val.equals("side") || val.equals("sides")) {
                flags |= (1 << CTMUtils.NORTH_FACE) | (1 << CTMUtils.SOUTH_FACE) | (1 << CTMUtils.EAST_FACE) | (1 << CTMUtils.WEST_FACE);
            } else if (val.equals("all")) {
                flags = -1;
            }
        }
        faces = flags;

        String tileList = properties.getProperty("tiles", "");
        int[] defaultTileMap = getDefaultTileMap();
        if (defaultTileMap == null) {
            if (tileList.equals("")) {
                MCPatcherUtils.error("no tile map given in %s.properties", filePrefix);
                tileMap = null;
            } else {
                tileMap = MCPatcherUtils.parseIntegerList(tileList);
                if (tileMap.length == 0) {
                    MCPatcherUtils.error("no tile map given in %s.properties", filePrefix);
                }
            }
        } else {
            if (tileList.equals("")) {
                tileMap = defaultTileMap;
            } else {
                tileMap = MCPatcherUtils.parseIntegerList(tileList);
                if (tileMap.length != defaultTileMap.length) {
                    MCPatcherUtils.error("tile map in %s.properties requires %d entries, got %d",
                        filePrefix, defaultTileMap.length, tileMap.length
                    );
                }
            }
        }
    }

    static int[] compose(int[] map1, int[] map2) {
        int[] newMap = new int[map2.length];
        for (int i = 0; i < map2.length; i++) {
            newMap[i] = map1[map2[i]];
        }
        return newMap;
    }

    static boolean shouldConnect(IBlockAccess blockAccess, int blockId, int i, int j, int k, int[] offset) {
        return blockAccess.getBlockId(i + offset[0], j + offset[1], k + offset[2]) == blockId;
    }

    boolean isValid() {
        return texture >= 0 && tileMap != null;
    }

    abstract int[] getDefaultTileMap();
    abstract int getTile(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face);
    
    static class Default extends TileOverride {
        private static final int[] defaultTileMap = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
            32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
        };

        // Index into this array is formed from these bit values:
        // 128 64  32
        // 1   *   16
        // 2   4   8
        private static final int[] neighborMap = new int[]{
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
        };

        private final int[] neighborTileMap;

        Default(String filePrefix, Properties properties) {
            super(filePrefix, properties);
            neighborTileMap = compose(tileMap, neighborMap);
        }

        @Override
        int[] getDefaultTileMap() {
            return defaultTileMap;
        }

        @Override
        int getTile(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
            int[][] offsets = CTMUtils.NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            for (int bit = 0; bit < 8; bit++) {
                if (shouldConnect(blockAccess, blockId, i, j, k, offsets[bit])) {
                    neighborBits |= (1 << bit);
                }
            }
            return neighborTileMap[neighborBits];
        }

    }

    static class Horizontal extends TileOverride {
        private static final int[] defaultTileMap = new int[]{
            12, 13, 14, 15,
        };

        // Index into this array is formed from these bit values:
        // 1   *   2
        private static final int[] neighborMap = new int[]{
            3, 2, 0, 1,
        };

        private final int[] neighborTileMap;

        Horizontal(String filePrefix, Properties properties) {
            super(filePrefix, properties);
            neighborTileMap = compose(tileMap, neighborMap);
        }

        @Override
        int[] getDefaultTileMap() {
            return defaultTileMap;
        }

        @Override
        int getTile(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
            if (face <= CTMUtils.TOP_FACE) {
                return -1;
            }
            int[][] offsets = CTMUtils.NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            if (shouldConnect(blockAccess, blockId, i, j, k, offsets[0])) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockAccess, blockId, i, j, k, offsets[4])) {
                neighborBits |= 2;
            }
            return neighborTileMap[neighborBits];
        }
    }

    static class Top extends TileOverride {
        private static final int[] defaultTileMap = new int[]{
            64, 65, 66, 67,
        };

        Top(String filePrefix, Properties properties) {
            super(filePrefix, properties);
        }

        @Override
        int[] getDefaultTileMap() {
            return defaultTileMap;
        }

        @Override
        int getTile(IBlockAccess blockAccess, int blockId, int origTexture, int i, int j, int k, int face) {
            if (face <= CTMUtils.TOP_FACE) {
                return -1;
            }
            if (blockAccess.getBlockMetadata(i, j, k) != 0) {
                return -1;
            }
            if (shouldConnect(blockAccess, blockId, i, j, k, CTMUtils.GO_UP)) {
                return tileMap[2];
            }
            return -1;
        }
    }
}
