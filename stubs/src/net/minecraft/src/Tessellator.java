package net.minecraft.src;

public class Tessellator {
    public static Tessellator instance;

    public static boolean convertQuadsToTriangles;

    public int rawBuffer[];
    public int rawBufferIndex;
    public int addedVertices;

    public int drawMode;
    public boolean isDrawing;

    public int texture; // added by ctm

    public Tessellator(int bufferSize) {
    }

    public void reset() {
    }

    public int draw() {
        return 0;
    }

    public void startDrawing(int drawMode) {
    }
}
