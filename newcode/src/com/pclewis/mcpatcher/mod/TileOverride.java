package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

abstract class TileOverride {
    final String filePrefix;
    final String textureName;
    final int texture;
    final int faces;
    final int metadata;
    final boolean connectByTile;
    final int[] tileMap;

    boolean disabled;

    static TileOverride create(String filePrefix, Properties properties, boolean connectByTile) {
        if (filePrefix == null) {
            return null;
        }
        if (properties == null) {
            InputStream is = null;
            try {
                is = CTMUtils.lastTexturePack.getInputStream(filePrefix + ".properties");
                if (is != null) {
                    properties = new Properties();
                    properties.load(is);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(is);
            }
        }
        if (properties == null) {
            return null;
        }

        if (connectByTile && !properties.contains("connect")) {
            properties.setProperty("connect", "tile");
        }
        String method = properties.getProperty("method", "default").trim().toLowerCase();
        TileOverride override = null;

        if (method.equals("default") || method.equals("glass") || method.equals("ctm")) {
            override = new CTM(filePrefix, properties);
        } else if (method.equals("random")) {
            override = new Random1(filePrefix, properties);
        } else if (method.equals("bookshelf") || method.equals("horizontal")) {
            override = new Horizontal(filePrefix, properties);
        } else if (method.equals("sandstone") || method.equals("top")) {
            override = new Top(filePrefix, properties);
        } else if (method.equals("repeat") || method.equals("pattern")) {
            override = new Repeat(filePrefix, properties);
        } else {
            MCPatcherUtils.error("%s.properties: unknown method \"%s\"", filePrefix, method);
        }

        return override == null || override.disabled ? null : override;
    }

    static TileOverride create(BufferedImage image) {
        TileOverride override = new CTM(image);
        return override.disabled ? null : override;
    }

    private TileOverride(BufferedImage image) {
        filePrefix = null;
        textureName = null;
        texture = MCPatcherUtils.getMinecraft().renderEngine.allocateAndSetupTexture(image);
        faces = -1;
        metadata = -1;
        connectByTile = true;
        tileMap = null;
    }

    private TileOverride(String filePrefix, Properties properties) {
        this.filePrefix = filePrefix;
        textureName = properties.getProperty("source", filePrefix + ".png");
        texture = CTMUtils.getTexture(textureName);
        if (texture < 0) {
            if (properties.contains("source")) {
                error("source texture %s not found", textureName);
            } else {
                disabled = true;
            }
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

        int meta = 0;
        for (int i : MCPatcherUtils.parseIntegerList(properties.getProperty("metadata", "0-31"), 0, 31)) {
            meta |= (1 << i);
        }
        metadata = meta;

        String connectType = properties.getProperty("connect", "tile").trim().toLowerCase();
        connectByTile = connectType.equals("tile");

        String tileList = properties.getProperty("tiles", "");
        int[] defaultTileMap = getDefaultTileMap();
        if (defaultTileMap == null) {
            if (tileList.equals("")) {
                error("no tile map given");
                tileMap = null;
            } else {
                tileMap = MCPatcherUtils.parseIntegerList(tileList, 0, 255);
                if (tileMap.length == 0) {
                    error("no tile map given");
                }
            }
        } else {
            if (tileList.equals("")) {
                tileMap = defaultTileMap;
            } else {
                tileMap = MCPatcherUtils.parseIntegerList(tileList, 0, 255);
                if (tileMap.length != defaultTileMap.length) {
                    error("tile map requires %d entries, got %d", defaultTileMap.length, tileMap.length);
                }
            }
        }
    }

    boolean requiresFace() {
        return true;
    }

    final void error(String format, Object... params) {
        if (filePrefix == null || filePrefix.equals("/ctm")) {
            //MCPatcherUtils.error(format, params);
        } else {
            MCPatcherUtils.error(filePrefix + ".properties: " + format, params);
        }
        disabled = true;
    }

    final boolean shouldConnect(IBlockAccess blockAccess, Block block, int tileNum, int i, int j, int k, int face, int[] offset) {
        i += offset[0];
        j += offset[1];
        k += offset[2];
        int neighborID = blockAccess.getBlockId(i, j, k);
        Block neighbor = Block.blocksList[neighborID];
        if (exclude(blockAccess, neighbor, tileNum, i, j, k, face)) {
            return false;
        } else if (connectByTile) {
            return neighbor.getBlockTexture(blockAccess, i, j, k, face) == tileNum;
        } else {
            return neighborID == block.blockID;
        }
    }

    final boolean exclude(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
        if (block == null) {
            return true;
        } else if ((faces & (1 << face)) == 0) {
            return true;
        } else if (metadata != -1) {
            int meta = blockAccess.getBlockMetadata(i, j, k);
            if (meta >= 0 && meta < 32 && (metadata & (1 << meta)) == 0) {
                return true;
            }
        }
        return false;
    }

    final int getTile(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
        if (face < 0) {
            if (requiresFace()) {
                error("method=%s is not supported for non-standard blocks", getMethod());
                return -1;
            } else {
                face = 0;
            }
        }
        if (exclude(blockAccess, block, origTexture, i, j, k, face)) {
            return -1;
        } else {
            return getTileImpl(blockAccess, block, origTexture, i, j, k, face);
        }
    }

    private static int[] compose(int[] map1, int[] map2) {
        int[] newMap = new int[map2.length];
        for (int i = 0; i < map2.length; i++) {
            newMap[i] = map1[map2[i]];
        }
        return newMap;
    }

    abstract String getMethod();

    abstract int[] getDefaultTileMap();

    abstract int getTileImpl(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face);

    final static class CTM extends TileOverride {
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

        private CTM(BufferedImage image) {
            super(image);
            neighborTileMap = compose(defaultTileMap, neighborMap);
        }

        private CTM(String filePrefix, Properties properties) {
            super(filePrefix, properties);
            neighborTileMap = compose(tileMap, neighborMap);
        }

        @Override
        String getMethod() {
            return "ctm";
        }

        @Override
        int[] getDefaultTileMap() {
            return defaultTileMap;
        }

        @Override
        int getTileImpl(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
            int[][] offsets = CTMUtils.NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            for (int bit = 0; bit < 8; bit++) {
                if (shouldConnect(blockAccess, block, origTexture, i, j, k, face, offsets[bit])) {
                    neighborBits |= (1 << bit);
                }
            }
            return neighborTileMap[neighborBits];
        }
    }

    final static class Horizontal extends TileOverride {
        private static final int[] defaultTileMap = new int[]{
            12, 13, 14, 15,
        };

        // Index into this array is formed from these bit values:
        // 1   *   2
        private static final int[] neighborMap = new int[]{
            3, 2, 0, 1,
        };

        private final int[] neighborTileMap;

        private Horizontal(String filePrefix, Properties properties) {
            super(filePrefix, properties);
            neighborTileMap = compose(tileMap, neighborMap);
        }

        @Override
        String getMethod() {
            return "horizontal";
        }

        @Override
        int[] getDefaultTileMap() {
            return defaultTileMap;
        }

        @Override
        int getTileImpl(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
            if (face <= CTMUtils.TOP_FACE) {
                return -1;
            }
            int[][] offsets = CTMUtils.NEIGHBOR_OFFSET[face];
            int neighborBits = 0;
            if (shouldConnect(blockAccess, block, origTexture, i, j, k, face, offsets[0])) {
                neighborBits |= 1;
            }
            if (shouldConnect(blockAccess, block, origTexture, i, j, k, face, offsets[4])) {
                neighborBits |= 2;
            }
            return neighborTileMap[neighborBits];
        }
    }

    final static class Top extends TileOverride {
        private static final int[] defaultTileMap = new int[]{
            66,
        };

        private Top(String filePrefix, Properties properties) {
            super(filePrefix, properties);
        }

        @Override
        String getMethod() {
            return "top";
        }

        @Override
        int[] getDefaultTileMap() {
            return defaultTileMap;
        }

        @Override
        int getTileImpl(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
            if (face <= CTMUtils.TOP_FACE) {
                return -1;
            }
            if (blockAccess.getBlockMetadata(i, j, k) != 0) {
                return -1;
            }
            if (shouldConnect(blockAccess, block, origTexture, i, j, k, face, CTMUtils.GO_UP)) {
                return tileMap[0];
            }
            return -1;
        }
    }

    final static class Random1 extends TileOverride {
        private static final long P1 = 0x1c3764a30115L;
        private static final long P2 = 0x227c1adccd1dL;
        private static final long P3 = 0xe0d251c03ba5L;
        private static final long P4 = 0xa2fb1377aeb3L;
        private static final long MULTIPLIER = 0x5deece66dL;
        private static final long ADDEND = 0xbL;

        private final int symmetry;
        private final int[] weight;
        private final int sum;

        private Random1(String filePrefix, Properties properties) {
            super(filePrefix, properties);

            String sym = properties.getProperty("symmetry", "none");
            if (sym.equals("all")) {
                symmetry = 6;
            } else if (sym.equals("opposite")) {
                symmetry = 2;
            } else {
                symmetry = 1;
            }

            boolean useWeight = false;
            int sum1 = 0;
            int[] wt = null;
            if (tileMap != null) {
                wt = new int[tileMap.length];
                String[] list = properties.getProperty("weights", "").split("\\s+");
                for (int i = 0; i < tileMap.length; i++) {
                    if (i < list.length && list[i].matches("^\\d+$")) {
                        wt[i] = Math.max(Integer.parseInt(list[i]), 0);
                    } else {
                        wt[i] = 1;
                    }
                    if (i > 0 && wt[i] != wt[0]) {
                        useWeight = true;
                    }
                    sum1 += wt[i];
                }
            }
            sum = sum1;
            if (useWeight && sum > 0) {
                weight = wt;
            } else {
                weight = null;
            }
        }

        @Override
        String getMethod() {
            return "random";
        }

        @Override
        int[] getDefaultTileMap() {
            return null;
        }

        @Override
        boolean requiresFace() {
            return false;
        }

        @Override
        int getTileImpl(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
            face /= symmetry;
            long n = P1 * i * (i + ADDEND) + P2 * j * (j + ADDEND) + P3 * k * (k + ADDEND) + P4 * face * (face + ADDEND);
            n = MULTIPLIER * (n + i + j + k + face) + ADDEND;
            int index = (int) ((n >> 32) ^ n) & 0x7fffffff;

            if (weight == null) {
                index %= tileMap.length;
            } else {
                int m = index % sum;
                for (index = 0; index < weight.length - 1 && m >= weight[index]; index++) {
                    m -= weight[index];
                }
            }
            return tileMap[index];
        }
    }

    final static class Repeat extends TileOverride {
        private final int width;
        private final int height;
        private final int symmetry;

        Repeat(String filePrefix, Properties properties) {
            super(filePrefix, properties);
            int w = 0;
            int h = 0;
            try {
                w = Integer.parseInt(properties.getProperty("width", "0"));
                h = Integer.parseInt(properties.getProperty("height", "0"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            width = w;
            height = h;
            if (width <= 0 || height <= 0 || width * height > CTMUtils.NUM_TILES) {
                error("invalid width and height (%dx%d)", width, height);
            } else if (tileMap.length != width * height) {
                error("must have exactly width * height (=%d) tiles, got %d", width * height, tileMap.length);
            }

            String sym = properties.getProperty("symmetry", "none");
            if (sym.equals("opposite")) {
                symmetry = ~1;
            } else {
                symmetry = -1;
            }
        }

        @Override
        String getMethod() {
            return "repeat";
        }

        @Override
        int[] getDefaultTileMap() {
            return null;
        }

        @Override
        boolean requiresFace() {
            return false;
        }

        @Override
        int getTileImpl(IBlockAccess blockAccess, Block block, int origTexture, int i, int j, int k, int face) {
            face &= symmetry;
            int x;
            int y;
            switch (face) {
                case CTMUtils.TOP_FACE:
                case CTMUtils.BOTTOM_FACE:
                    x = i;
                    y = k;
                    break;

                case CTMUtils.NORTH_FACE:
                    x = -i - 1;
                    y = -j;
                    break;

                case CTMUtils.SOUTH_FACE:
                    x = i;
                    y = -j;
                    break;

                case CTMUtils.WEST_FACE:
                    x = k;
                    y = -j;
                    break;

                case CTMUtils.EAST_FACE:
                    x = -k - 1;
                    y = -j;
                    break;

                default:
                    return -1;
            }
            x %= width;
            if (x < 0) {
                x += width;
            }
            y %= height;
            if (y < 0) {
                y += height;
            }
            return tileMap[width * y + x];
        }
    }
}
