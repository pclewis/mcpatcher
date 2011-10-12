package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Entity;

import java.util.ArrayList;
import java.util.HashMap;

public class MobRandomizer {
    private static HashMap<String, ArrayList<String>> mobHash = new HashMap<String, ArrayList<String>>();

    public static void reset() {
        MCPatcherUtils.log("reset random mobs list");
        mobHash.clear();
    }

    public static String randomTexture(Entity entity) {
        String texture = entity.getEntityTexture();
        if (!texture.startsWith("/mob/") || !texture.endsWith(".png")) {
            return texture;
        }
        ArrayList<String> variations = mobHash.get(texture);
        if (variations == null) {
            variations = new ArrayList<String>();
            variations.add(texture);
            for (int i = 2; ; i++) {
                String s = texture.replace(".png", "" + i + ".png");
                if (TextureUtils.hasResource(s)) {
                    variations.add(s);
                } else {
                    break;
                }
            }
            mobHash.put(texture, variations);
        }
        return variations.get(entity.entityId % variations.size());
    }
}
