package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.WorldChunkManager;

abstract class BiomeHelper {
    static BiomeHelper instance;

    IBlockAccess blockAccess;

    BiomeHelper(IBlockAccess blockAccess) {
        this.blockAccess = blockAccess;
    }

    abstract BiomeGenBase getBiomeGenAt(int i, int j, int k);

    abstract float getTemperature(int i, int j, int k);

    abstract float getRainfall(int i, int j, int k);

    static class Old extends BiomeHelper {
        WorldChunkManager chunkManager;

        Old(IBlockAccess blockAccess) {
            super(blockAccess);
            chunkManager = blockAccess.getWorldChunkManager();
        }

        @Override
        BiomeGenBase getBiomeGenAt(int i, int j, int k) {
            return chunkManager.getBiomeGenAt(i, k);
        }

        @Override
        float getTemperature(int i, int j, int k) {
            return chunkManager.getTemperature(i, j, k);
        }

        @Override
        float getRainfall(int i, int j, int k) {
            return chunkManager.getRainfall(i, k);
        }
    }

    static class New extends BiomeHelper {
        private static boolean logged;

        private BiomeGenBase lastBiome;
        private float lastTemperature;
        private float lastRainfall;
        private int lastI;
        private int lastK;

        New(IBlockAccess blockAccess) {
            super(blockAccess);
            if (!logged) {
                logged = true;
                MCPatcherUtils.log("biomes v1.2 detected");
            }
        }

        @Override
        BiomeGenBase getBiomeGenAt(int i, int j, int k) {
            checkLast(i, j, k);
            return lastBiome;
        }

        @Override
        float getTemperature(int i, int j, int k) {
            checkLast(i, j, k);
            return lastTemperature;
        }

        @Override
        float getRainfall(int i, int j, int k) {
            checkLast(i, j, k);
            return lastRainfall;
        }
        
        private void checkLast(int i, int j, int k) {
            if (lastBiome == null || i != lastI || k != lastK) {
                lastI = i;
                lastK = k;
                lastBiome = blockAccess.getBiomeGenAt(i, k);
                lastTemperature = lastBiome.getTemperaturef();
                lastRainfall = lastBiome.getRainfallf();
            }
        }
    }
}
