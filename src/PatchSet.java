import javassist.CtClass;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

public class PatchSet {
    private String className;
    private PatchSpec[] patchSpecs;

    public PatchSet(String className, PatchSpec[] patchSpecs) {
        this.className = className;
        this.patchSpecs = patchSpecs;
    }

	public PatchSet(String className, PatchSet src) {
		this(src);
	    this.className = className;
	}

	public PatchSet(PatchSet src) {
		this.className = src.className;
		this.patchSpecs = new PatchSpec[src.patchSpecs.length];
		for(int i = 0; i < src.patchSpecs.length; ++i) {
			this.patchSpecs[i] = new PatchSpec(src.patchSpecs[i]);
		}
	}

	public void setParam(String key, String value) {
		for(PatchSpec ps :  patchSpecs) {
			ps.getPatch().setParam(key, value);
		}
	}

	public String getClassName() {
		return className;
	}

	public PatchSpec[] getPatchSpecs() {
		return patchSpecs;
	}

    public void visitConstPool(ConstPool cp) throws Exception {
	    for(PatchSpec ps :  patchSpecs) {
		    ps.getPatch().visitConstPool(cp);
	    }
    }

    public void visitMethod(MethodInfo mi) throws Exception{
	    for(PatchSpec ps :  patchSpecs) {
		    ps.getPatch().visitMethod(mi);
	    }
    }

    public void visitClassPre(CtClass ct) throws Exception {
        for(PatchSpec ps : patchSpecs) {
            ps.getPatch().visitClassPre(ct);
        }
    }

    public void visitClassPost(CtClass ct) throws Exception {
        for(PatchSpec ps : patchSpecs) {
            ps.getPatch().visitClassPost(ct);
        }
    }
}
