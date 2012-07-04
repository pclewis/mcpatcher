package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Entity;
import net.minecraft.src.TexturePackBase;

public class MobOverlay {
    private static final String MOOSHROOM_OVERLAY = "/mob/redcow_overlay.png";
    private static final String SNOWMAN_OVERLAY = "/mob/snowman_overlay.png";

    private static boolean overlayActive;
    private static int overlayCounter;
    private static boolean haveMooshroom;
    private static boolean haveSnowman;

    static void reset(TexturePackBase texturePack) {
        haveMooshroom = MCPatcherUtils.readImage(texturePack.getInputStream(MOOSHROOM_OVERLAY)) != null;
        haveSnowman = MCPatcherUtils.readImage(texturePack.getInputStream(SNOWMAN_OVERLAY)) != null;
    }

    public static String setupMooshroom(Entity entity, String defaultTexture) {
        overlayCounter = 0;
        if (haveMooshroom) {
            overlayActive = true;
            return MobRandomizer.randomTexture(entity, MOOSHROOM_OVERLAY);
        } else {
            overlayActive = false;
            return defaultTexture;
        }
    }

    public static boolean renderMooshroomOverlay() {
        overlayCounter++;
        return overlayActive;
    }

    public static void finish() {
        overlayCounter = 0;
        overlayActive = false;
    }
}
