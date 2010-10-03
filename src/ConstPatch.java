import com.sun.javaws.exceptions.InvalidArgumentException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstPatch extends Patch {
    int appliedCount;
	ArrayList<Patch> subpatches = new ArrayList<Patch>();
	int tag;
	Object from;
	Object to;

	public String getDescription() { return "Change constant value"; }
    public ParamSpec[] getParamSpecs() { return Patches.PSPEC_TILESIZE; }

	private class UsePatch extends BytecodePatch {
		public String getDescription() { return "Update const reference " + fi + " -> " + ti; }
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

	public ConstPatch(Object from, Object to)  {
		assert(from.getClass().equals(to.getClass()));
		this.tag = getTag(from);
		this.from = from;
		this.to = to;
	}

	private int getTag(Object o) {
		if(o instanceof Float)  { return ConstPool.CONST_Float;  }
		if(o instanceof Double) { return ConstPool.CONST_Double; }
		throw new AssertionError("Unreachable");
	}

	private int addToPool(ConstPool cp, Object o) {
		if(o instanceof Float)  { return cp.addFloatInfo((Float)o);   }
		if(o instanceof Double) { return cp.addDoubleInfo((Double)o); }
		throw new AssertionError("Unreachable");
	}

	private boolean checkEqual(ConstPool cp, int index, Object o) {
		if(o instanceof Float)  { return cp.getFloatInfo(index)  == (Float)o;  }
		if(o instanceof Double) { return cp.getDoubleInfo(index) == (Double)o; }
		throw new AssertionError("Unreachable");
	}

	public void visitConstPool(ConstPool cp) {
		int oldIndex = -1;
		for(int i = 1; i < cp.getSize(); ++i) {
			if( cp.getTag(i) == this.tag ) {
				if(checkEqual(cp, i, this.from.getClass().cast(this.from))) {
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
		MCPatcher.out.println("  " + this.getDescription());
    }

    public void visitMethod(MethodInfo mi) throws Exception {
		for ( Patch p : subpatches )
			p.visitMethod(mi);
    }
}
