import javassist.bytecode.ConstPool;

/**
 * Created by IntelliJ IDEA.
 * User: nex
 * Date: Oct 4, 2010
 * Time: 9:40:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConstPoolUtils {
	static int getTag(Object o) {
		if(o instanceof Float)   { return ConstPool.CONST_Float;   }
		if(o instanceof Double)  { return ConstPool.CONST_Double;  }
		if(o instanceof Integer) { return ConstPool.CONST_Integer; }
		throw new AssertionError("Unreachable");
	}

	static int addToPool(ConstPool cp, Object o) {
		if(o instanceof Float)   { return cp.addFloatInfo((Float)o);     }
		if(o instanceof Double)  { return cp.addDoubleInfo((Double)o);   }
		if(o instanceof Integer) { return cp.addIntegerInfo((Integer)o); }
		throw new AssertionError("Unreachable");
	}

	static boolean checkEqual(ConstPool cp, int index, Object o) {
		if(o instanceof Float)   { return cp.getFloatInfo(index)  == (Float)o;  }
		if(o instanceof Double)  { return cp.getDoubleInfo(index) == (Double)o; }
		if(o instanceof Integer) { return cp.getIntegerInfo(index) == (Integer)o; }
		throw new AssertionError("Unreachable");
	}
}
