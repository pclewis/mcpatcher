package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.TexturePackAPI;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.NBTTagCompound;

import java.lang.reflect.Method;
import java.util.*;

public class MobRandomizer {
    private static final HashMap<String, MobEntry> mobHash = new HashMap<String, MobEntry>();

    private static final long MULTIPLIER = 0x5deece66dL;
    private static final long ADDEND = 0xbL;
    private static final long MASK = (1L << 48) - 1;

    static {
        TexturePackAPI.ChangeHandler.register(new TexturePackAPI.ChangeHandler(MCPatcherUtils.RANDOM_MOBS, 2) {
            @Override
            protected void onChange() {
                mobHash.clear();
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
        MobEntry mobEntry = mobHash.get(texture);
        if (mobEntry == null) {
            mobEntry = new MobEntry(texture);
            mobHash.put(texture, mobEntry);
        }
        return mobEntry.getSkin(info.skin, info.origX, info.origY, info.origZ, info.origBiome);
    }

    public static final class ExtraInfo {
        private static Method getBiomeNameAt;
        private static final HashMap<Integer, ExtraInfo> allInfo = new HashMap<Integer, ExtraInfo>();

        public final long skin;
        public final int origX;
        public final int origY;
        public final int origZ;
        public final String origBiome;

        static {
            try {
                Class<?> biomeHelperClass = Class.forName(MCPatcherUtils.BIOME_HELPER_CLASS);
                getBiomeNameAt = biomeHelperClass.getDeclaredMethod("getBiomeNameAt", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            } catch (Throwable e) {
            }
        }

        ExtraInfo(EntityLiving entity) {
            skin = getSkinId(entity.entityId);
            origX = (int) entity.posX;
            origY = (int) entity.posY;
            origZ = (int) entity.posZ;
            origBiome = getBiome(origX, origY, origZ);
        }

        ExtraInfo(long skin, int origX, int origY, int origZ) {
            this.skin = skin;
            this.origX = origX;
            this.origY = origY;
            this.origZ = origZ;
            origBiome = getBiome(origX, origY, origZ);
        }

        static ExtraInfo getInfo(EntityLiving entity) {
            synchronized (allInfo) {
                if (entity.randomMobsInfo == null) {
                    entity.randomMobsInfo = allInfo.get(entity.entityId);
                    if (entity.randomMobsInfo == null) {
                        entity.randomMobsInfo = new ExtraInfo(entity);
                        putInfo(entity);
                    }
                }
            }
            return entity.randomMobsInfo;
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

        @Override
        public String toString() {
            return String.format("%s{%d, %d, %d, %d, %s}", getClass().getName(), skin, origX, origY, origZ, origBiome);
        }

        private static long getSkinId(int entityId) {
            long n = entityId;
            n = n ^ (n << 16) ^ (n << 32) ^ (n << 48);
            n = MULTIPLIER * n + ADDEND;
            n = MULTIPLIER * n + ADDEND;
            n &= MASK;
            return (n >> 32) ^ n;
        }

        private static String getBiome(int x, int y, int z) {
            if (getBiomeNameAt != null) {
                try {
                    String biome = (String) getBiomeNameAt.invoke(null, x, y, z);
                    if (biome != null) {
                        return biome.toLowerCase().replace(" ", "");
                    }
                } catch (Throwable e) {
                    getBiomeNameAt = null;
                    e.printStackTrace();
                }
            }
            return null;
        }

        public static void readFromNBT(EntityLiving entity, NBTTagCompound nbt) {
            long skin = nbt.getLong("randomMobsSkin");
            if (skin == 0) {
                entity.randomMobsInfo = new ExtraInfo(entity);
            } else {
                entity.randomMobsInfo = new ExtraInfo(skin, nbt.getInteger("origX"), nbt.getInteger("origY"), nbt.getInteger("origZ"));
            }
            putInfo(entity);
        }

        public static void writeToNBT(EntityLiving entity, NBTTagCompound nbt) {
            ExtraInfo info = getInfo(entity);
            if (info != null) {
                nbt.setLong("randomMobsSkin", info.skin);
                nbt.setInteger("origX", info.origX);
                nbt.setInteger("origY", info.origY);
                nbt.setInteger("origZ", info.origZ);
            }
        }
    }

    private static class MobEntry {
        final String baseSkin;
        final ArrayList<String> allSkins;
        final int skinCount;
        final ArrayList<SkinEntry> entries;

        MobEntry(String baseSkin) {
            this.baseSkin = baseSkin;
            allSkins = new ArrayList<String>();
            allSkins.add(baseSkin);
            for (int i = 2; ; i++) {
                final String skin = baseSkin.replace(".png", "" + i + ".png");
                if (!TexturePackAPI.hasResource(skin)) {
                    break;
                }
                allSkins.add(skin);
            }
            skinCount = allSkins.size();
            if (skinCount <= 1) {
                entries = null;
                return;
            }
            MCPatcherUtils.debug("found %d variations for %s", skinCount, baseSkin);

            String filename = baseSkin.replace(".png", ".properties");
            String altFilename = filename.replaceFirst("_(eyes|overlay|tame|angry)\\.properties$", ".properties");
            Properties properties = TexturePackAPI.getProperties(filename);
            if (properties == null && !filename.equals(altFilename)) {
                properties = TexturePackAPI.getProperties(altFilename);
                if (properties != null) {
                    MCPatcherUtils.debug("using %s for %s", altFilename, baseSkin);
                }
            }
            ArrayList<SkinEntry> tmpEntries = new ArrayList<SkinEntry>();
            if (properties != null) {
                for (int i = 0; ; i++) {
                    SkinEntry entry = SkinEntry.load(properties, i, skinCount);
                    if (entry == null) {
                        if (i > 0) {
                            break;
                        }
                    } else {
                        MCPatcherUtils.debug("  %s", entry.toString());
                        tmpEntries.add(entry);
                    }
                }
            }
            entries = tmpEntries.isEmpty() ? null : tmpEntries;
        }

        String getSkin(long key, int i, int j, int k, String biome) {
            if (entries == null) {
                int index = (int) (key % skinCount);
                if (index < 0) {
                    index += skinCount;
                }
                return allSkins.get(index);
            } else {
                for (SkinEntry entry : entries) {
                    if (entry.match(i, j, k, biome)) {
                        int n = entry.skins.length;
                        int index = (int) (key % n);
                        if (index < 0) {
                            index += n;
                        }
                        return allSkins.get(entry.skins[index]);
                    }
                }
            }
            return baseSkin;
        }
    }

    private static class SkinEntry {
        final int[] skins;
        final HashSet<String> biomes;
        final int minHeight;
        final int maxHeight;

        static SkinEntry load(Properties properties, int index, int limit) {
            String skinList = properties.getProperty("skins." + index, "").trim().toLowerCase();
            int[] skins;
            if (skinList.equals("*") || skinList.equals("all") || skinList.equals("any")) {
                skins = new int[limit];
                for (int i = 0; i < skins.length; i++) {
                    skins[i] = i;
                }
            } else {
                skins = MCPatcherUtils.parseIntegerList(skinList, 1, limit);
                if (skins.length <= 0) {
                    return null;
                }
                for (int i = 0; i < skins.length; i++) {
                    skins[i]--;
                }
            }

            HashSet<String> biomes = new HashSet<String>();
            String biomeList = properties.getProperty("biomes." + index, "").trim().toLowerCase();
            if (!biomeList.equals("")) {
                Collections.addAll(biomes, biomeList.split("\\s+"));
            }
            if (biomes.isEmpty()) {
                biomes = null;
            }

            int maxHeight = -1;
            int minHeight = -1;
            try {
                maxHeight = Integer.parseInt(properties.getProperty("maxHeight." + index, "-1").trim());
                minHeight = Integer.parseInt(properties.getProperty("minHeight." + index, "-1").trim());
                if (minHeight < 0 || minHeight > maxHeight) {
                    minHeight = -1;
                    maxHeight = -1;
                }
            } catch (NumberFormatException e) {
            }

            return new SkinEntry(skins, biomes, minHeight, maxHeight);
        }

        SkinEntry(int[] skins, HashSet<String> biomes, int minHeight, int maxHeight) {
            this.skins = skins;
            this.biomes = biomes;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        boolean match(int i, int j, int k, String biome) {
            if (biomes != null) {
                if (!biomes.contains(biome)) {
                    return false;
                }
            }
            if (minHeight >= 0) {
                if (j < minHeight || j > maxHeight) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("skins:");
            for (int i : skins) {
                sb.append(' ').append(i + 1);
            }
            if (biomes != null) {
                sb.append(", biomes:");
                for (String s : biomes) {
                    sb.append(' ').append(s);
                }
            }
            if (minHeight >= 0) {
                sb.append(", height: ").append(minHeight).append('-').append(maxHeight);
            }
            return sb.toString();
        }
    }
}
