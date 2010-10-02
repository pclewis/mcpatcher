import com.sun.javaws.exceptions.InvalidArgumentException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Patch {
    public Params params = new Params(new Params.MissHandler() {
	    public String get(String key) {
		    for (ParamSpec p : getParamSpecs()) {
		        if(p.name.equals(key)) {
		            return MCPatcher.globalParams.get(p.defaultSource);
		        }
		    }
		    return null;
	    }
    });

    abstract public ParamSpec[] getParamSpecs();
    abstract public String getDescription();

    abstract public void visitConstPool(ConstPool cp) throws Exception;
    abstract public void visitMethod(MethodInfo mi) throws Exception;


    public void setParam(String key, String value) {
        params.put(key, value);
    }

    protected byte b(int value, int index) {
        return (byte)((value >> (index*8)) & 0xFF);
    }
}