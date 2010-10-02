import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstPatch<T> extends Patch {
    int appliedCount;
	ArrayList<Patch> subpatches = new ArrayList<Patch>();
	int tag;
	T from;
	T to;

	public String getDescription() { return "Change constant value"; }
    public ParamSpec[] getParamSpecs() { return Patches.PSPEC_TILESIZE; }

	private class UsePatch extends BytecodePatch {
		public String getDescription() { return ""; }
		public ParamSpec[] getParamSpecs() { return Patches.PSPEC_EMPTY; }
		private int op, fi, ti;

		private UsePatch(int op, int fi, int ti) {
			this.op = op;
			this.fi = fi;
			this.ti = ti;
		}

		byte[] getBytes(int i) {
			if(op==LDC)
				return new byte[]{ (byte)op, b(i, 0) };
			else
				return new byte[]{ (byte)op, b(i, 1), b(i, 0) };
		}

		byte[] getFromBytes() throws Exception { return getBytes(fi); }
		byte[] getToBytes() throws Exception { return getBytes(ti); }
	}

	public ConstPatch(int tag, T from, T to) {
		this.tag = tag;
		this.from = from;
		this.to = to;
	}

	private int addToPool(ConstPool cp, Float to) { return cp.addFloatInfo(to); }

	private int addToPool(ConstPool cp, Object o) {
		throw new UnsupportedOperationException();
	}

	private boolean checkEqual(ConstPool cp, int index, Float from) { return cp.getFloatInfo(index) == from; }

	private boolean checkEqual(ConstPool cp, int index, Object o) {
		throw new UnsupportedOperationException();
	}

	public void visitConstPool(ConstPool cp) {
		int oldIndex = -1;
		for(int i = 1; i < cp.getSize(); ++i) {
			if( cp.getTag(i) == this.tag ) {
				if(checkEqual(cp, i, this.from)) {
					oldIndex = i;
					break;
				}
			}
		}

		if(oldIndex <= 0)
			return;

		int newIndex = this.addToPool(cp, this.to);

		subpatches.clear();
		subpatches.add(new UsePatch(UsePatch.LDC, oldIndex, newIndex));
		subpatches.add(new UsePatch(UsePatch.LDC2_W, oldIndex, newIndex));

		appliedCount += 1;
    }

    public void visitMethod(MethodInfo mi) throws Exception {
		for ( Patch p : subpatches )
			p.visitMethod(mi);
    }
}
