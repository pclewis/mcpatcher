package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Entity;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TexturePackBase;

public class MobOverlay {
    private static final String MOOSHROOM_OVERLAY = "/mob/redcow_overlay.png";
    private static final String SNOWMAN_OVERLAY = "/mob/snowman_overlay.png";

    private static final double MOO_X0 = -0.45;
    private static final double MOO_X1 = 0.45;
    private static final double MOO_Y0 = -0.5;
    private static final double MOO_Y1 = 0.5;
    private static final double MOO_Z0 = -0.45;
    private static final double MOO_Z1 = 0.45;

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
        if (overlayActive && overlayCounter < 3) {
            float tileX0 = overlayCounter / 3.0f;
            float tileX1 = ++overlayCounter / 3.0f;

            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(MOO_X0, MOO_Y1, MOO_Z0, tileX0, 0.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y0, MOO_Z0, tileX0, 1.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y0, MOO_Z1, tileX1, 1.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y1, MOO_Z1, tileX1, 0.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y1, MOO_Z1, tileX0, 0.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y0, MOO_Z1, tileX0, 1.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y0, MOO_Z0, tileX1, 1.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y1, MOO_Z0, tileX1, 0.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y1, MOO_Z1, tileX0, 0.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y0, MOO_Z1, tileX0, 1.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y0, MOO_Z0, tileX1, 1.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y1, MOO_Z0, tileX1, 0.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y1, MOO_Z0, tileX0, 0.0);
            tessellator.addVertexWithUV(MOO_X1, MOO_Y0, MOO_Z0, tileX0, 1.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y0, MOO_Z1, tileX1, 1.0);
            tessellator.addVertexWithUV(MOO_X0, MOO_Y1, MOO_Z1, tileX1, 0.0);
            tessellator.draw();
        }
        return overlayActive;
    }

    public static void finish() {
        overlayCounter = 0;
        overlayActive = false;
    }
}
