package net.minecraft.src;

public class World {
    public WorldInfo worldInfo;
    public WorldProvider worldProvider;
    public int lightningFlash;

    public float getSunAngle(float f) {
        return 1.0f;
    }

    public float getCelestialAngle(float f) {
        return 1.0f;
    }

    public long getWorldTime() {
        return 0L;
    }
}
