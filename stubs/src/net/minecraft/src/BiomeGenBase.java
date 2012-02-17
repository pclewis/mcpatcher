package net.minecraft.src;

public class BiomeGenBase {
    public int biomeID;
    public String biomeName;
    public int color;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier;

    public float getTemperaturef() { // 1.2 and up
        return 0.0f;
    }

    public float getRainfallf() { // 1.2 and up
        return 0.0f;
    }
}
