import com.sun.javaws.exceptions.InvalidArgumentException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Patch {
    HashMap<String,String> params = new HashMap<String,String>();

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

    protected String getParam(String key) throws InvalidArgumentException {
        if(params.containsKey(key)) {
            return params.get(key);
        } else {
            for (ParamSpec p : getParamSpecs()) {
                if(p.name.equals(key)) {
                    return MCPatcher.getDefaultParam(p.defaultSource);
                }
            }
        }
        throw new InvalidArgumentException(new String[]{"Invalid parameter: " + key});
    }

    protected int getParamInt(String key) throws InvalidArgumentException {
        return Integer.parseInt(getParam(key), 10);
    }

    protected byte getParamByte(String key, int index) throws InvalidArgumentException {
        return b(getParamInt(key), index);
    }
}