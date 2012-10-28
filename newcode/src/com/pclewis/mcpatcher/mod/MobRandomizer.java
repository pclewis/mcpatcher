package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.TexturePackAPI;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.NBTTagCompound;

import java.lang.reflect.Method;
import java.util.HashMap;

public class MobRandomizer {
    static {
        TexturePackAPI.ChangeHandler.register(new TexturePackAPI.ChangeHandler(MCPatcherUtils.RANDOM_MOBS, 2) {
            @Override
            protected void onChange() {
                MobRuleList.clear();
                MobOverlay.reset();
            }
        });
    }

    public static String randomTexture(EntityLiving entity) {
        return randomTexture(entity, entity.getEntityTexture());
    }

    public static String randomTexture(EntityLiving entity, String texture) {
        if (!texture.startsWith("/mob/") || !texture.endsWith(".png")) {
            return texture;
        }
        ExtraInfo info = ExtraInfo.getInfo(entity);
        MobRuleList list = MobRuleList.get(texture);
        return list.getSkin(info.skin, info.origX, info.origY, info.origZ, info.origBiome);
    }

    public static String randomTexture(Object entity, String texture) {
        if (entity instanceof EntityLiving) {
            return randomTexture((EntityLiving) entity, texture);
        } else {
            return texture;
        }
    }

    public static final class ExtraInfo {
        private static final String SKIN_TAG = "randomMobsSkin";
        private static final String ORIG_X_TAG = "origX";
        private static final String ORIG_Y_TAG = "origY";
        private static final String ORIG_Z_TAG = "origZ";

        private static final long MULTIPLIER = 0x5deece66dL;
        private static final long ADDEND = 0xbL;
        private static final long MASK = (1L << 48) - 1;

        private static Method getBiomeNameAt;
        private static final HashMap<Integer, ExtraInfo> allInfo = new HashMap<Integer, ExtraInfo>();

        public final long skin;
        public final int origX;
        public final int origY;
        public final int origZ;
        public String origBiome;

        static {
            try {
                Class<?> biomeHelperClass = Class.forName(MCPatcherUtils.BIOME_HELPER_CLASS);
                getBiomeNameAt = biomeHelperClass.getDeclaredMethod("getBiomeNameAt", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            } catch (Throwable e) {
            }
            if (getBiomeNameAt == null) {
                MCPatcherUtils.warn("%s biome integration failed", MCPatcherUtils.RANDOM_MOBS);
            } else {
                MCPatcherUtils.info("%s biome integration active", MCPatcherUtils.RANDOM_MOBS);
            }
        }

        ExtraInfo(EntityLiving entity) {
            skin = getSkinId(entity.entityId);
            origX = (int) entity.posX;
            origY = (int) entity.posY;
            origZ = (int) entity.posZ;
            setBiome();
        }

        ExtraInfo(long skin, int origX, int origY, int origZ) {
            this.skin = skin;
            this.origX = origX;
            this.origY = origY;
            this.origZ = origZ;
            setBiome();
        }

        private void setBiome() {
            if (origBiome == null && getBiomeNameAt != null) {
                try {
                    String biome = (String) getBiomeNameAt.invoke(null, origX, origY, origZ);
                    if (biome != null) {
                        origBiome = biome.toLowerCase().replace(" ", "");
                    }
                } catch (Throwable e) {
                    getBiomeNameAt = null;
                    e.printStackTrace();
                }
            }
        }

        @Override
        public String toString() {
            return String.format("%s{%d, %d, %d, %d, %s}", getClass().getSimpleName(), skin, origX, origY, origZ, origBiome);
        }

        static ExtraInfo getInfo(EntityLiving entity) {
            ExtraInfo info = entity.randomMobsInfo;
            synchronized (allInfo) {
                if (info == null) {
                    info = allInfo.get(entity.entityId);
                    if (info == null) {
                        info = new ExtraInfo(entity);
                        putInfo(entity);
                    }
                    entity.randomMobsInfo = info;
                }
            }
            info.setBiome();
            return info;
        }

        static void putInfo(EntityLiving entity) {
            synchronized (allInfo) {
                allInfo.put(entity.entityId, entity.randomMobsInfo);
            }
        }

        static void clearInfo() {
            synchronized (allInfo) {
                allInfo.clear();
            }
        }

        private static long getSkinId(int entityId) {
            long n = entityId;
            n = n ^ (n << 16) ^ (n << 32) ^ (n << 48);
            n = MULTIPLIER * n + ADDEND;
            n = MULTIPLIER * n + ADDEND;
            n &= MASK;
            return (n >> 32) ^ n;
        }

        public static void readFromNBT(EntityLiving entity, NBTTagCompound nbt) {
            long skin = nbt.getLong(SKIN_TAG);
            if (skin == 0) {
                entity.randomMobsInfo = new ExtraInfo(entity);
            } else {
                int x = nbt.getInteger(ORIG_X_TAG);
                int y = nbt.getInteger(ORIG_Y_TAG);
                int z = nbt.getInteger(ORIG_Z_TAG);
                entity.randomMobsInfo = new ExtraInfo(skin, x, y, z);
            }
            putInfo(entity);
        }

        public static void writeToNBT(EntityLiving entity, NBTTagCompound nbt) {
            ExtraInfo info = getInfo(entity);
            if (info != null) {
                nbt.setLong(SKIN_TAG, info.skin);
                nbt.setInteger(ORIG_X_TAG, info.origX);
                nbt.setInteger(ORIG_Y_TAG, info.origY);
                nbt.setInteger(ORIG_Z_TAG, info.origZ);
            }
        }
    }
}
