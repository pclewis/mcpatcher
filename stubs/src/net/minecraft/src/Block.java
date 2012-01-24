package net.minecraft.src;

public class Block {
    public static Block blocksList[];
    public static int lightValue[];

    public int blockID;

    public boolean renderAsNormalBlock() {
        return false;
    }

    public float blockStrength(EntityPlayer thePlayer) {
        return 0;  //To change body of created methods use File | Settings | File Templates.
    }
}
