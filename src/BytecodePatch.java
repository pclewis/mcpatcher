import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public abstract class BytecodePatch extends Patch implements javassist.bytecode.Opcode {
	int appliedCount;

    abstract byte[] getFromBytes() throws Exception;
    abstract byte[] getToBytes() throws Exception;

    public void visitConstPool(ConstPool cp) {}

    public void visitMethod(MethodInfo mi) throws Exception {
        CodeAttribute ca = mi.getCodeAttribute();
        if(ca == null || ca.getCodeLength() <= 0)
            return;

        String code = new String(ca.getCode(), "ISO-8859-1");
        String fromstr = new String(this.getFromBytes(), "ISO-8859-1");
        String tostr = new String(this.getToBytes(), "ISO-8859-1");
        String new_code = code.replace(fromstr, tostr);

        if(!new_code.equals(code)) {
            ca.iterator().write(new_code.getBytes("ISO-8859-1"), 0);
            appliedCount += 1;
        }
    }
}
