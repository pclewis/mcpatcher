import com.sun.javaws.exceptions.InvalidArgumentException;

class Patches {
    private ParamSpec[] PS_TILESIZE = new ParamSpec[]{ new ParamSpec("tileSize", "tileSize", "Tile size") };

    class ArraySizePatch extends BytecodePatch {
        int multiplier = 1;
        int addX = 0;
        int addY = 0;

        public String getDescription() { return "Fix array allocations"; }

        public ParamSpec[] getParamSpecs() { return PS_TILESIZE; }

        public ArraySizePatch() { this(1); }
        public ArraySizePatch(int multiplier) { this(multiplier, 0, 0); }
        public ArraySizePatch(int multiplier, int addX, int addY) {
            this.multiplier = multiplier;
            this.addX = addX;
            this.addY = addY;
        }

        public byte[] bytes(int dim) {
            int size = ((dim+addX)*(dim+addY))*multiplier;
            return new byte[]{
                SIPUSH, b(size, 0), b(size, 1),
                (byte)NEWARRAY
            };
        }

        public byte[] getFromBytes() throws Exception {
            return bytes(16);
        }

        public byte[] getToBytes() throws Exception {
            return bytes(getParamInt("tileSize"));
        }
    }

    class WhilePatch extends BytecodePatch {
        int multiplier = 1;
        boolean square = false;

        public String getDescription() { return "Fix while loops"; }

        public ParamSpec[] getParamSpecs() { return PS_TILESIZE; }

        public WhilePatch() { this(1, false); }
        public WhilePatch(int multiplier) { this(multiplier, false); }
        public WhilePatch(boolean square) { this(1, square); }
        public WhilePatch(int multiplier, boolean square) {
            this.multiplier = multiplier;
            this.square = square;
        }

        public byte[] bytes(int cnt) {
            if(square) cnt = cnt * cnt;
            cnt = cnt * multiplier;
            if(cnt<=0xFF) {
                return new byte[]{
                    BIPUSH, b(cnt, 0),
                    (byte)IF_ICMPGE
                };
            } else {
                return new byte[]{
                    SIPUSH, b(cnt, 1), b(cnt, 0),
                    (byte)IF_ICMPGE
                };
            }
        }

        public byte[] getFromBytes() throws Exception {
            return bytes(16);
        }

        public byte[] getToBytes() throws Exception {
            return bytes(getParamInt("tileSize"));
        }
    }


    final PatchSet waterPatches = new PatchSet(
            "Water",
            new PatchSpec[] {
                    new PatchSpec(new ArraySizePatch(), true),
                    new PatchSpec(new WhilePatch(), true),
                    new PatchSpec(new WhilePatch(true), true),
            }
    );
}