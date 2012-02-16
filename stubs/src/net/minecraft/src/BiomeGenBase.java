package net.minecraft.src;

public class BiomeGenBase {
    public int biomeID;
    public String biomeName;
    public int color;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier;
    
    public int getTemperaturef() { // 1.2 and up
        return 0;
    }
    
    public int getRainfallf() { // 1.2 and up
        return 0;
    }
}
