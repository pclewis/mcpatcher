package net.minecraft.src;

public class Tessellator {
    public static Tessellator instance;

    public static boolean convertQuadsToTriangles;

    public int rawBuffer[];
    public boolean hasNormals;
    public int rawBufferIndex;
    public int addedVertices;
    public int drawMode;

    public boolean preserve; // added by ctm

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
}
