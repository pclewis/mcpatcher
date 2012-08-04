package net.minecraft.src;

public abstract class Entity {
    public int entityId;
    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    public double lastTickPosX;
    public double lastTickPosY;
    public double lastTickPosZ;
    public boolean onGround;
    public boolean isDead;
    public float rotationYaw;
    public float fallDistance;
    public final AxisAlignedBB boundingBox = null;
    public net.minecraft.src.World worldObj;

    public Entity(World worldObj) {
    }

    protected abstract void entityInit();

    public abstract void readEntityFromNBT(NBTTagCompound var1);

    public abstract void writeEntityToNBT(NBTTagCompound var1);

    public boolean isEntityBurning() {
        return false;
    }

    public boolean isEntityAlive() {
        return false;
    }

    public boolean isInWater() {
        return false;
    }

    public void setEntityDead() {
    }

    public boolean ignoreFrustumCheck;

    public String getEntityTexture() {
        return null;
    }

    public void setPosition(double posX, double posY, double posZ) {
    }

    public void onUpdate() {
    }
}
