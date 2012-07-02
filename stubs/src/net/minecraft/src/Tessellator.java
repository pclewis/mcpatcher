package net.minecraft.src;

public class Tessellator {
    public static Tessellator instance;

    public static boolean convertQuadsToTriangles;

    public int rawBuffer[];
    public int rawBufferIndex;
    public int addedVertices;
    public int vertexCount;
    public int bufferSize;
    public int drawMode;
    public boolean isDrawing;

    public int texture; // added by ctm

    public Tessellator(int bufferSize) {
    }

    public Tessellator() { // forge replaces 1-arg constructor
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

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
    }
}
