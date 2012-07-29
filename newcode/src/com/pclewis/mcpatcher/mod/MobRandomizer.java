package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Entity;
import net.minecraft.src.TexturePackBase;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MobRandomizer {
    private static final HashMap<String, ArrayList<GenericEntry>> mobHash = new HashMap<String, ArrayList<GenericEntry>>();
    private static TexturePackBase lastTexturePack;

    private static final long MULTIPLIER = 0x5deece66dL;
    private static final long ADDEND = 0xbL;
    private static final long MASK = (1L << 48) - 1;

    private static final Method getBiomeNameAt;

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

    public static String randomTexture(Entity entity) {
        return randomTexture(entity, entity.getEntityTexture());
    }

    public static String randomTexture(Entity entity, String texture) {
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
        for (GenericEntry entry : getEntries(texture)) {
            if (entry.match(entity.origX, entity.origY, entity.origZ)) {
                int index = (int) (entity.randomMobsSkin % entry.variations.size());
                if (index < 0) {
                    index += entry.variations.size();
                }
                return entry.variations.get(index);
            }
        }
        return texture;
    }

    private static long getSkinId(int entityId) {
        long n = entityId;
        n = n ^ (n << 16) ^ (n << 32) ^ (n << 48);
        n = MULTIPLIER * n + ADDEND;
        n = MULTIPLIER * n + ADDEND;
        n &= MASK;
        return (n >> 32) ^ n;
    }

    private static ArrayList<GenericEntry> getEntries(String texture) {
        ArrayList<GenericEntry> entries = mobHash.get(texture);
        if (entries != null) {
            return entries;
        }
        entries = new ArrayList<GenericEntry>();
        GenericEntry genericEntry = new GenericEntry(texture);
        int count = genericEntry.variations.size();

        InputStream is = null;
        try {
            is = lastTexturePack.getInputStream(texture.replace(".png", ".properties"));
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                for (Map.Entry<Object, Object> property : properties.entrySet()) {
                    String k = property.getKey().toString();
                    String v = property.getValue().toString();
                    if (k.startsWith("height.")) {
                        String[] tokens = k.substring(7).split("\\s*-\\s*");
                        if (tokens.length == 2) {
                            try {
                                int min = Integer.parseInt(tokens[0]);
                                int max = Integer.parseInt(tokens[1]);
                                entries.add(new HeightEntry(texture, count, v, min, max));
                            } catch (NumberFormatException e) {
                            }
                        }
                    } else if (k.startsWith("biome.")) {
                        entries.add(new BiomeEntry(texture, count, v, k.substring(6)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(is);
        }

        entries.add(genericEntry);
        if (count > 1) {
            MCPatcherUtils.debug("found %d variations for %s", count, texture);
        }
        mobHash.put(texture, entries);
        return entries;
    }

    private static class GenericEntry {
        final ArrayList<String> variations;

        GenericEntry(String baseTexture) {
            variations = new ArrayList<String>();
            variations.add(baseTexture);
            for (int i = 2; ; i++) {
                final String t = baseTexture.replace(".png", "" + i + ".png");
                if (MCPatcherUtils.readImage(lastTexturePack.getInputStream(t)) != null) {
                    variations.add(t);
                } else {
                    break;
                }
            }
        }

        GenericEntry(String baseTexture, int n, String list) {
            int[] l = MCPatcherUtils.parseIntegerList(list, 1, n);
            variations = new ArrayList<String>();
            for (int i : l) {
                if (i <= 1) {
                    variations.add(baseTexture);
                } else {
                    variations.add(baseTexture.replace(".png", "" + i + ".png"));
                }
            }
        }

        boolean match(int i, int j, int k) {
            return true;
        }
    }

    private static class HeightEntry extends GenericEntry {
        final int min;
        final int max;

        HeightEntry(String baseTexture, int n, String list, int min, int max) {
            super(baseTexture, n, list);
            this.min = min;
            this.max = max;
        }

        @Override
        boolean match(int i, int j, int k) {
            return j >= min && j <= max;
        }
    }

    private static class BiomeEntry extends GenericEntry {
        final String biome;

        BiomeEntry(String baseTexture, int n, String list, String biome) {
            super(baseTexture, n, list);
            this.biome = biome;
        }

        @Override
        boolean match(int i, int j, int k) {
            if (getBiomeNameAt != null) {
                try {
                    return biome.equals(getBiomeNameAt.invoke(null, i, j, k));
                } catch (Throwable e) {
                }
            }
            return false;
        }
    }
}
