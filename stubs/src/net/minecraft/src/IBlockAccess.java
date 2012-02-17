package net.minecraft.src;

public interface IBlockAccess {
    public BiomeGenBase getBiomeGenAt(int i, int k); // 1.2 and up

    public WorldChunkManager getWorldChunkManager(); // 1.1 and below
}
