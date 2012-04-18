package net.minecraft.src;

public class Tessellator {
    public static Tessellator instance;

    public static boolean convertQuadsToTriangles;

    public int rawBuffer[];
    public int rawBufferIndex;
    public int addedVertices;

    public double textureU;
    public double textureV;
    public int brightness;
    public int color;
    public boolean hasColor;
    public boolean hasTexture;
    public boolean hasBrightness;
    public boolean hasNormals;
    public boolean isColorDisabled;
    public int drawMode;
    public double xOffset;
    public double yOffset;
    public double zOffset;
    public int normal;
    public boolean isDrawing;

    public Tessellator(int bufferSize) {
    }

    public void reset() {
    }

    public int draw() {
        return 0;
    }

    public void startDrawing(int drawMode) {
    }

    public void startDrawingQuads() {
    }

    public void setNormal(float x, float y, float z) {
    }

    public void setBrightness(int brightness) {
    }

    public void setColorRGBA(int r, int g, int b, int a) {
    }

    public void setTextureUV(double u, double v) {
    }

    public void setTranslation(double x, double y, double z) {
    }
}
