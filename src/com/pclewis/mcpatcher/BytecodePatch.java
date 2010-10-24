package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public abstract class BytecodePatch extends Patch implements javassist.bytecode.Opcode {
	int appliedCount;

	protected static byte[] ldc(int i) {
		return ConstPoolUtils.getLoad(LDC, i);
	}

	protected static byte[] push(MethodInfo mi, int value) {
		if(value == 0 ) {
			return new byte[] { ICONST_0 };
		} else if (value == 1) {
			return new byte[] { ICONST_1 };
		} if(value <= Byte.MAX_VALUE) {
			return new byte[] { BIPUSH, (byte)value };
		} else if (value <= Short.MAX_VALUE) {
			return new byte[] { SIPUSH, Util.b(value, 1), Util.b(value, 0) };
		} else {
			int index = ConstPoolUtils.findOrAdd(mi.getConstPool(), value);
			return ldc(index);
		}
	}

	protected static byte[] buildCode(Object... values) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(values.length);
		for(Object v : values) {
			if(v instanceof Integer) {
				baos.write(((Integer) v).byteValue());
			} else if(v instanceof Byte) {
				baos.write((Byte)v);
			} else if (v instanceof byte[]) {
				try {
					baos.write((byte[])v);
				} catch(IOException e) {
					throw new RuntimeException("impossible", e);
				}
			} else {
				throw new RuntimeException("Unknown type");
			}
		}
		return baos.toByteArray();
	}

	abstract byte[] getFromBytes(MethodInfo mi) throws Exception;
    abstract byte[] getToBytes(MethodInfo mi) throws Exception;

    public void visitConstPool(ConstPool cp) {}

	private boolean matchAt(CodeIterator ci, int start, byte[] from) {
		for (int i = 0; i < from.length; ++i) {
			if((byte)ci.byteAt(start+i) != from[i])
				return false;
		}
		return true;
	}

	private void pad(CodeIterator ci, int start, int length) {
		for(int i = 0; i < length; ++i)
			ci.writeByte(NOP, start+i);
	}
    public void visitMethod(MethodInfo mi) throws Exception {
        CodeAttribute ca = mi.getCodeAttribute();
        if(ca == null || ca.getCodeLength() <= 0)
            return;

	    CodeIterator ci = ca.iterator();
	    byte[] from = this.getFromBytes(mi);
	    byte[] to = this.getToBytes(mi);
	    if(Arrays.equals(from,to)) return;

	    try {
		    int lastPatchEnd = 0;
			while(ci.hasNext()) {
				int i = ci.next();
				if(i<lastPatchEnd)
					continue;
				if(matchAt(ci, i, from)) {
					int skip = 0;
					if(to.length <= from.length) {
						skip = from.length - to.length;
						pad(ci, i, skip);
					} else {
						int gapNeeded = to.length - from.length;
						int gapSize = ci.insertGap(i, gapNeeded);
						skip = (gapSize - gapNeeded);
					}
					ci.write(to, i + skip);
					ci.move(i + skip);
					lastPatchEnd = i + skip + to.length;
					appliedCount += 1;
					MCPatcher.out.println("  " + this.getDescription() + " - " + mi.getName() + "@" + i);
				}
			}
	    } catch (BadBytecode e) {
		    throw new RuntimeException("Generated bad bytecode", e);
	    }
    }
}
