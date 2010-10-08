import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

abstract class BytecodeTilePatch extends BytecodePatch {
    int multiplier = 1;
    int addX = 0;
    int addY = 0;
	int add = 0;
    boolean square = false;
    boolean zero = false;

    public ParamSpec[] getParamSpecs() {
        return zero ? Patches.PSPEC_EMPTY : Patches.PSPEC_TILESIZE;
    }

	public void visitConstPool(ConstPool cp) {
		int toSize = getToSize();
		if(toSize <= Short.MAX_VALUE)
			return;

		ConstPoolUtils.findOrAdd(cp, toSize);
	}

	byte[] getBytes(int tileSize) {
		return new byte[]{};
	};

	byte[] getBytes(int tileSize, MethodInfo mi) {
		return getBytes(tileSize);
	}

    private int calc(int orig) {
        int result = orig;
	    if(square) result = (result+addX)*(result+addY);
	    result = result * multiplier;
	    result = result + this.add;
	    return result;
    }

	public int getFromSize() {
		return calc(16);
	}

	public int getToSize() {
		return zero ? 0 : calc(params.getInt("tileSize"));
	}

    public byte[] getFromBytes(MethodInfo mi) throws Exception {
        return getBytes(getFromSize(), mi);
    }

    public byte[] getToBytes(MethodInfo mi) throws Exception {
        return getBytes(getToSize(), mi);
    }

    public BytecodeTilePatch multiplier(int multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    public BytecodeTilePatch addX(int addX) {
        this.addX = addX;
        return this;
    }

    public BytecodeTilePatch addY(int addY) {
        this.addY = addY;
        return this;
    }

    public BytecodeTilePatch square(boolean square) {
        this.square = square;
        return this;
    }

    public BytecodeTilePatch zero(boolean zero) {
        this.zero = zero;
        return this;
    }

	public BytecodeTilePatch add(int add) {
	    this.add = add;
	    return this;
	}

}
