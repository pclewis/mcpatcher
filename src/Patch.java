import javassist.CtClass;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

public abstract class Patch implements Cloneable {
    public Params params;

    abstract public ParamSpec[] getParamSpecs();
    abstract public String getDescription();

    public void visitClassPre(CtClass ct) throws Exception {}
    public void visitClassPost(CtClass ct) throws Exception {}
    public void visitConstPool(ConstPool cp) throws Exception {}
    public void visitMethod(MethodInfo mi) throws Exception {}

	public Patch() {
		this.params = new Params(new Params.MissHandler() {
			public String get(String key) {
				for (ParamSpec p : getParamSpecs()) {
					if(p.name.equals(key)) {
						return MCPatcher.globalParams.get(p.defaultSource);
					}
				}
				return null;
			}
	    });
	}

    public void setParam(String key, String value) {
        params.put(key, value);
    }

	public Patch clone() throws CloneNotSupportedException {
		Patch newPatch = (Patch)super.clone();
		newPatch.params = new Params(this.params);
		return newPatch;
	}

	// convenience
	protected byte[] fc(MethodInfo mi, Object c       ) { return fc(mi.getConstPool(), c);    }
	protected byte[] fc(MethodInfo mi, Object c, int n) { return fc(mi.getConstPool(), c, n); }
	protected byte[] fc(ConstPool  cp, Object c       ) { return fc(cp, c, 2               ); }
	protected byte[] fc(ConstPool cp, Object c, int n) {
		int i = ConstPoolUtils.findOrAdd(cp, c);
		assert(i > 0);
		return Util.bytes(i,n);
	}

	protected byte[] load(MethodInfo mi, Object c) { return load(mi.getConstPool(), c);    }
	protected byte[] load(ConstPool cp, Object c)  { return ConstPoolUtils.getLoad(cp, c); }
}