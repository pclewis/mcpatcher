import javassist.bytecode.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PatchDefinition {
	public String fileName;
	public Replacement[] replacements;
	public ConstChanger[] changers;

	public PatchDefinition(String fileName, Replacement[] replacements, ConstChanger[] changers) {
		this.fileName = fileName;
		this.replacements = replacements;
		this.changers = changers;
	}
	public PatchDefinition(String fileName, Replacement[] replacements) {
		this(fileName, replacements, new ConstChanger[]{});
	}

	public void apply(InputStream input, OutputStream output) throws IOException {
		ClassFile cf = new ClassFile(new DataInputStream(input));
		ConstPool cp = cf.getConstPool();
		ArrayList<Replacement> rps = new ArrayList<Replacement>(Arrays.asList(this.replacements));

		for(ConstChanger cc : changers) {
			int new_index = cc.add(cp);
			rps.add(
				new Replacement("Change const " + cc.description(),
			        new int[] {
				        Bytecode.LDC2_W, (cc.index()>>8)&0xFF, (cc.index())&0xFF
			        },
                    new int[]{
	                    Bytecode.LDC2_W, (new_index>>8)&0xFF, new_index&0xFF
                    }
				)
			);
			rps.add(
				new Replacement("Change const " + cc.description(),
			        new int[] {
				        Bytecode.LDC, (cc.index())&0xFF
			        },
                    new int[]{
	                    Bytecode.LDC, new_index&0xFF
                    }
				)
			);
		}

		for(Object mo : cf.getMethods()) {
			MethodInfo mi = (MethodInfo) mo;
			CodeAttribute ca = mi.getCodeAttribute();
			if(ca == null || ca.getCodeLength() <= 0)
				continue;
			String code = new String(ca.getCode(), "ISO-8859-1");
			String orig_code = code;
			for(Replacement r : rps) {
				String from = new String(r.from, "ISO-8859-1");
				String to = new String(r.to, "ISO-8859-1");
				String new_code = code.replace(from, to);
				if(!new_code.equals(code)) {
					code = new_code;
					MCPatcher.out.println( "  " + mi.getName() + " -> " + r.description );
				}
			}

			if(!code.equals(orig_code)) {
				ca.iterator().write(code.getBytes("ISO-8859-1"), 0);
			}
		}

		if(changers.length!=0)
			cf.compact();

		cf.write(new DataOutputStream(output));
	}
}