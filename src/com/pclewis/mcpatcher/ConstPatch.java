package com.pclewis.mcpatcher;

import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.util.ArrayList;

public class ConstPatch extends Patch {
    int appliedCount;
	ArrayList<Patch> subpatches;
	private int tag;

	private Object from;
	private Object to;

	public String getDescription() { return "Change constant value " + getFrom().toString() + " -> " + getTo().toString(); }
    public ParamSpec[] getParamSpecs() { return Patches.PSPEC_TILESIZE; }

	private class UsePatch extends BytecodePatch {
		public String getDescription() { return "Update const reference " + fi + " -> " + ti; }
		public ParamSpec[] getParamSpecs() { return Patches.PSPEC_EMPTY; }
		private int op, fi, ti;

		public UsePatch(int op, int fi, int ti) {
			this.op = op;
			this.fi = fi;
			this.ti = ti;
		}

		byte[] getBytes(int i) {
			int mop = op;
			if(i>=0xFF && mop==LDC)
				mop = LDC_W;
			if(mop==LDC)
				return new byte[]{ (byte)mop, Util.b(i, 0) };
			else
				return new byte[]{ (byte)mop, Util.b(i, 1), Util.b(i, 0) };
		}

		byte[] getFromBytes(MethodInfo mi) throws Exception { return getBytes(fi); }
		byte[] getToBytes(MethodInfo mi) throws Exception { return getBytes(ti); }
	}

	private class UseImmPatch extends UsePatch {
		int loadop;
		public UseImmPatch(int loadop, int op, int ti) {
			super(op, -1, ti);
			this.loadop = loadop;
		}
		byte[] getFromBytes(MethodInfo mi) throws Exception { return new byte[] { (byte) loadop }; }
	}

	protected ConstPatch() {
		this.subpatches = new ArrayList<Patch>();
	}

	public ConstPatch(Object from, Object to)  {
		assert(from.getClass().equals(to.getClass()));
		this.tag = ConstPoolUtils.getTag(from);
		this.from = from;
		this.to = to;
		this.subpatches = new ArrayList<Patch>();
	}

	public ConstPatch clone() {
		ConstPatch newPatch = null;
		try {
			newPatch = (ConstPatch)super.clone();
		} catch(CloneNotSupportedException e) {
			throw new RuntimeException("Can't clone", e);
		}
		newPatch.subpatches =  new ArrayList<Patch>();
		return newPatch;
	}

	protected int getTag() {
		return tag;
	}

	protected Object getFrom() {
		return from;
	}

	protected Object getTo() {
		return to;
	}

	public void visitConstPool(ConstPool cp) {
		int oldIndex = -1;
		int newIndex = ConstPoolUtils.addToPool(cp, this.getTo());
		boolean handled = false;

		subpatches.clear();

		if(this.getFrom() instanceof Float) {
			float f = (Float)this.getFrom();
			handled = true;
			if(f == 0.0F) {
				subpatches.add(new UseImmPatch(UsePatch.FCONST_0, UsePatch.LDC, newIndex));
			} else if (f == 1.0F) {
				subpatches.add(new UseImmPatch(UsePatch.FCONST_1, UsePatch.LDC, newIndex));
			} else {
				handled = false;
			}
		} else if(this.getFrom() instanceof Double) {
			double d = (Double)this.getFrom();
			handled = true;
			if(d == 0.0D) {
				subpatches.add(new UseImmPatch(UsePatch.DCONST_0, UsePatch.LDC2_W, newIndex));
			} else if (d == 1.0D) {
				subpatches.add(new UseImmPatch(UsePatch.DCONST_1, UsePatch.LDC2_W, newIndex));
			} else {
				handled = false;
			}
		}

		if(!handled) {
			for(int i = 1; i < cp.getSize(); ++i) {
				if( cp.getTag(i) == this.getTag()) {
					if(ConstPoolUtils.checkEqual(cp, i, this.getFrom().getClass().cast(this.getFrom()))) {
						oldIndex = i;
						break;
					}
				}
			}

			if(oldIndex <= 0) {
				//throw new RuntimeException("Can't find constant: " + from.toString() );
				return;
			}

			if(this.from instanceof ClassRef) {
				subpatches.add(new UsePatch(UsePatch.NEW, oldIndex, newIndex));
			} else if (this.from instanceof MethodRef) {
				subpatches.add(new UsePatch(UsePatch.INVOKESPECIAL, oldIndex, newIndex));
				subpatches.add(new UsePatch(UsePatch.INVOKEVIRTUAL, oldIndex, newIndex));
			} else {
				subpatches.add(new UsePatch(UsePatch.LDC, oldIndex, newIndex));
				subpatches.add(new UsePatch(UsePatch.LDC2_W, oldIndex, newIndex));
			}
		}

		appliedCount += 1;
		MCPatcher.out.println("  " + this.getDescription());
    }

    public void visitMethod(MethodInfo mi) throws Exception {
		for ( Patch p : subpatches )
			p.visitMethod(mi);
    }
}
