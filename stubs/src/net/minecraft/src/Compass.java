package net.minecraft.src;

import net.minecraft.client.Minecraft;

public class Compass extends TextureFX {
    public double currentAngle;
    public double targetAngle;

    public Compass(Minecraft minecraft) {
        super(0);
    }
}
