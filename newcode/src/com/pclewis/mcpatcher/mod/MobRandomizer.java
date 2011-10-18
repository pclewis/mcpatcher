package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Entity;
import net.minecraft.src.TexturePackBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MobRandomizer {
    private static final Random random = new Random();
    private static final HashMap<String, ArrayList<String>> mobHash = new HashMap<String, ArrayList<String>>();
    private static TexturePackBase lastTexturePack;

    public static void reset() {
        MCPatcherUtils.log("reset random mobs list");
        mobHash.clear();
    }

    public static String randomTexture(Entity entity) {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;
        if (lastTexturePack != selectedTexturePack) {
            lastTexturePack = selectedTexturePack;
            reset();
        }
        String texture = entity.getEntityTexture();
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
            mobHash.put(texture, variations);
        }
        return variations.get(getVariant(entity.entityId, variations.size()));
    }

    private static int getVariant(int entityId, int max) {
        if (max < 2) {
            return 0;
        } else {
            random.setSeed(entityId);
            return random.nextInt(max);
        }
    }
}
