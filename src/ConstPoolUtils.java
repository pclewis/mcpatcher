import javassist.bytecode.Bytecode;
import javassist.bytecode.ConstPool;

public class ConstPoolUtils {
	static int getTag(Object o) {
		if(o instanceof Float)   { return ConstPool.CONST_Float;   }
		if(o instanceof Double)  { return ConstPool.CONST_Double;  }
		if(o instanceof Integer) { return ConstPool.CONST_Integer; }
		if(o instanceof String)  { return ConstPool.CONST_String;  }
		throw new AssertionError("Unreachable");
	}

	static int addToPool(ConstPool cp, Object o) {
		if(o instanceof Float)   { return cp.addFloatInfo((Float)o);     }
		if(o instanceof Double)  { return cp.addDoubleInfo((Double)o);   }
		if(o instanceof Integer) { return cp.addIntegerInfo((Integer)o); }
		if(o instanceof String)  { return cp.addStringInfo((String)o);   }
		throw new AssertionError("Unreachable");
	}

	static boolean checkEqual(ConstPool cp, int index, Object o) {
		if(o instanceof Float)   { return cp.getFloatInfo(index)  == (Float)o;  }
		if(o instanceof Double)  { return cp.getDoubleInfo(index) == (Double)o; }
		if(o instanceof Integer) { return cp.getIntegerInfo(index) == (Integer)o; }
		if(o instanceof String)  { return ((String)o).equals(cp.getStringInfo(index)); }
		throw new AssertionError("Unreachable");
	}

	static int find(ConstPool cp, Object value) {
		int index = -1;
		int tag = getTag(value);
		for(int i = 1; i < cp.getSize(); ++i) {
			if( cp.getTag(i) == tag ) {
				if(checkEqual(cp, i, value)) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	static int findOrAdd(ConstPool cp, Object value) {
		int index = ConstPoolUtils.find(cp, value);
		if(index == -1)
			index = ConstPoolUtils.addToPool(cp, value);
		return index;
	}

	static byte[] getLoad(ConstPool cp, Object value) {
		int index = findOrAdd(cp, value);
		int op = Bytecode.LDC;
		if(value instanceof Double) {
			op = Bytecode.LDC2_W;
		}
		return getLoad(op, index);
	}
	static byte[] getLoad(int op, int i) {
		int mop = op;
		if(i>=Byte.MAX_VALUE && mop==Bytecode.LDC)
			mop = Bytecode.LDC_W;
		if(mop==Bytecode.LDC)
			return new byte[]{ (byte)mop, Util.b(i, 0) };
		else
			return new byte[]{ (byte)mop, Util.b(i, 1), Util.b(i, 0) };
	}
}