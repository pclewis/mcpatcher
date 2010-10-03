import javassist.bytecode.*;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public abstract class BytecodePatch extends Patch implements javassist.bytecode.Opcode {
	int appliedCount;

    abstract byte[] getFromBytes() throws Exception;
    abstract byte[] getToBytes() throws Exception;

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
	    byte[] from = this.getFromBytes();
	    byte[] to = this.getToBytes();
	    try {
			while(ci.hasNext()) {
				int i = ci.next();
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
					appliedCount += 1;
					MCPatcher.out.println("  " + this.getDescription() + " - " + mi.getName() + "@" + i);
				}
			}
	    } catch (BadBytecode e) {
		    throw new RuntimeException("Generated bad bytecode", e);
	    }
    }
}
