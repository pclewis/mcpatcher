package net.minecraft.src;

public class FontRenderer {
    public boolean isUnicode;
    public float[] charWidthf;
    
    public int getCharWidth(char c) { // 1.2.4 and up
        return 0;
    }

    public void initialize(GameSettings gameSettings, String font, RenderEngine renderEngine) {
    }
}
