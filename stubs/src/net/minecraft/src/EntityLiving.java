package net.minecraft.src;

public class EntityLiving extends Entity {
    protected int health;

    // added by Random Mobs
    public long randomMobsSkin;
    public boolean randomMobsSkinSet;
    public int origX;
    public int origY;
    public int origZ;
    public String origBiome;

    public EntityLiving(World worldObj) {
        super(worldObj);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound var1) {
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound var1) {
    }
}
