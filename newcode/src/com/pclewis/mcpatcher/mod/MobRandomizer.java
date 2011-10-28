package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Entity;
import net.minecraft.src.TexturePackBase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MobRandomizer {
    private static final HashMap<String, ArrayList<String>> mobHash = new HashMap<String, ArrayList<String>>();
    private static TexturePackBase lastTexturePack;

    private static final long MULTIPLIER = 0x5deece66dL;
    private static final long ADDEND = 0xbL;
    private static final long MASK = (1L << 48) - 1;

    public static void reset() {
        MCPatcherUtils.log("reset random mobs list");
        mobHash.clear();
    }

    public static String randomTexture(Entity entity) {
        return randomTexture(entity, entity.getEntityTexture());
    }

    public static String randomTexture(Entity entity, String texture) {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;
        if (lastTexturePack != selectedTexturePack) {
            lastTexturePack = selectedTexturePack;
            reset();
        }
        if (lastTexturePack == null || !texture.startsWith("/mob/") || !texture.endsWith(".png")) {
            return texture;
        }
        ArrayList<String> variations = mobHash.get(texture);
        if (variations == null) {
            variations = new ArrayList<String>();
            variations.add(texture);
            for (int i = 2; ; i++) {
                String s = texture.replace(".png", "" + i + ".png");
                boolean hasResource = false;
                InputStream inputStream = null;
                try {
                    inputStream = lastTexturePack.getInputStream(s);
                    if (inputStream != null) {
                        hasResource = true;
                    }
                } catch (Throwable e) {
                } finally {
                    MCPatcherUtils.close(inputStream);
                }
                if (hasResource) {
                    variations.add(s);
                } else {
                    break;
                }
            }
            if (variations.size() > 1) {
                MCPatcherUtils.log("found %d variations for %s", variations.size(), texture);
            }
            mobHash.put(texture, variations);
        }
        if (!entity.randomMobsSkinSet) {
            entity.randomMobsSkin = getSkinId(entity.entityId);
            entity.randomMobsSkinSet = true;
        }
        return variations.get((int) (entity.randomMobsSkin % variations.size()));
    }

    private static long getSkinId(int entityId) {
        long n = entityId;
        n = n ^ (n << 16) ^ (n << 32) ^ (n << 48);
        n = (MULTIPLIER * n + ADDEND) & MASK;
        return (n >> 32) ^ n;
    }
}
