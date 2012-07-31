package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.TexturePackBase;

import java.lang.reflect.Method;
import java.util.*;

public class MobRandomizer {
    private static final HashMap<String, MobInfo> mobHash = new HashMap<String, MobInfo>();
    private static TexturePackBase lastTexturePack;

    private static final long MULTIPLIER = 0x5deece66dL;
    private static final long ADDEND = 0xbL;
    private static final long MASK = (1L << 48) - 1;

    private static Method getBiomeNameAt;

    static {
        Method method = null;
        try {
            Class<?> biomeHelperClass = Class.forName(MCPatcherUtils.BIOME_HELPER_CLASS);
            method = biomeHelperClass.getDeclaredMethod("getBiomeNameAt", Integer.TYPE, Integer.TYPE, Integer.TYPE);
        } catch (Throwable e) {
        }
        getBiomeNameAt = method;
    }

    public static void reset() {
        MCPatcherUtils.debug("reset random mobs list");
        mobHash.clear();
        MobOverlay.reset(lastTexturePack);
    }

    public static String randomTexture(EntityLiving entity) {
        return randomTexture(entity, entity.getEntityTexture());
    }

    public static String randomTexture(EntityLiving entity, String texture) {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.getSelectedTexturePack();
        if (lastTexturePack != selectedTexturePack) {
            lastTexturePack = selectedTexturePack;
            reset();
        }
        if (lastTexturePack == null || !texture.startsWith("/mob/") || !texture.endsWith(".png")) {
            return texture;
        }
        if (!entity.randomMobsSkinSet) {
            entity.randomMobsSkin = getSkinId(entity.entityId);
            entity.origX = (int) entity.posX;
            entity.origY = (int) entity.posY;
            entity.origZ = (int) entity.posZ;
            entity.randomMobsSkinSet = true;
        }
        if (entity.origBiome == null && getBiomeNameAt != null) {
            try {
                entity.origBiome = (String) getBiomeNameAt.invoke(null, entity.origX, entity.origY, entity.origZ);
            } catch (Throwable e) {
                getBiomeNameAt = null;
                e.printStackTrace();
            }
            if (entity.origBiome != null) {
                entity.origBiome = entity.origBiome.toLowerCase();
            }
        }
        MobInfo mobInfo = mobHash.get(texture);
        if (mobInfo == null) {
            mobInfo = new MobInfo(texture);
            mobHash.put(texture, mobInfo);
        }
        return mobInfo.getSkin(entity.randomMobsSkin, entity.origX, entity.origY, entity.origZ, entity.origBiome);
    }

    private static long getSkinId(int entityId) {
        long n = entityId;
        n = n ^ (n << 16) ^ (n << 32) ^ (n << 48);
        n = MULTIPLIER * n + ADDEND;
        n = MULTIPLIER * n + ADDEND;
        n &= MASK;
        return (n >> 32) ^ n;
    }

    private static class MobInfo {
        final String baseSkin;
        final ArrayList<String> allSkins;
        final int skinCount;
        final ArrayList<SkinEntry> entries;

        MobInfo(String baseSkin) {
            this.baseSkin = baseSkin;
            allSkins = new ArrayList<String>();
            allSkins.add(baseSkin);
            for (int i = 2; ; i++) {
                final String skin = baseSkin.replace(".png", "" + i + ".png");
                if (MCPatcherUtils.readImage(lastTexturePack.getInputStream(skin)) == null) {
                    break;
                }
                allSkins.add(skin);
            }
            skinCount = allSkins.size();
            if (skinCount > 1) {
                MCPatcherUtils.debug("found %d variations for %s", skinCount, baseSkin);
            }

            String filename = baseSkin.replace(".png", ".properties");
            Properties properties = MCPatcherUtils.readProperties(lastTexturePack.getInputStream(filename));
            if (properties == null && (filename.contains("_eyes") || filename.contains("_overlay"))) {
                filename = filename.replace("_eyes", "").replace("_overlay", "");
                properties = MCPatcherUtils.readProperties(lastTexturePack.getInputStream(filename));
                if (properties != null) {
                    MCPatcherUtils.debug("using %s for %s", filename, baseSkin);
                }
            }
            ArrayList<SkinEntry> tmpEntries = new ArrayList<SkinEntry>();
            if (properties != null) {
                for (int i = 0; ; i++) {
                    SkinEntry entry = SkinEntry.load(properties, i, skinCount);
                    if (entry == null) {
                        break;
                    }
                    tmpEntries.add(entry);
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
            String skinList = properties.getProperty("skins." + index, "").trim();
            int[] skins = MCPatcherUtils.parseIntegerList(skinList, 1, limit);
            if (skins.length <= 0) {
                return null;
            }
            for (int i = 0; i < skins.length; i++) {
                skins[i]--;
            }

            String[] biomes = properties.getProperty("biomes." + index, "").trim().toLowerCase().split("\\s+");
            if (biomes.length <= 0) {
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

        SkinEntry(int[] skins, String[] biomes, int minHeight, int maxHeight) {
            this.skins = skins;
            this.biomes = new HashSet<String>();
            Collections.addAll(this.biomes, biomes);
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        boolean match(int i, int j, int k, String biome) {
            if (biomes != null) {
                if (biome == null || !biomes.contains(biome.toLowerCase())) {
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
    }
}
