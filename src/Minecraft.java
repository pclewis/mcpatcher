import javassist.bytecode.*;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Minecraft implements Opcode {
	public JarFile getJar() {
		return jar;
	}

	private JarFile jar;
	File file;

	private HashMap<String,String> classMap = new HashMap<String,String>();
	private HashMap<String,ClassFinder> classFinders = new HashMap<String, ClassFinder>() {{
		put("AnimManager",new MethodCallSignature("glTexSubImage2D"));
		put("AnimTexture",new BytecodeSignature(new byte[]{
			(byte)SIPUSH, 0x04, 0x00, // 1024
			(byte)NEWARRAY, T_BYTE
		}));
		put("Compass", new BytecodeSignature(new byte[]{
			(byte)SIPUSH, 0x01, 0x40, // 320
			(byte)NEWARRAY, T_FLOAT,
			(byte)PUTFIELD, 0x00, 0x28, // these will probably change..
			(byte)ALOAD_0,
			(byte)SIPUSH, 0x01, 0x40, // 320
			(byte)NEWARRAY, T_FLOAT,
			(byte)PUTFIELD, 0x00, 0x29,
			(byte)ALOAD_0,
			(byte)SIPUSH, 0x01, 0x00, // 256
			(byte)NEWARRAY, T_INT,
			(byte)PUTFIELD, 0x00, 0x2B,
			(byte)ALOAD_0,	
		}));
		put("Fire", new BytecodeSignature(new byte[]{
			(byte)SIPUSH, 0x01, 0x40, // 320
			(byte)NEWARRAY, T_FLOAT,
			(byte)PUTFIELD, 0x00, 0x14,
			(byte)ALOAD_0,
			(byte)SIPUSH, 0x01, 0x40, // 320
			(byte)NEWARRAY, T_FLOAT,
			(byte)PUTFIELD, 0x00, 0x15,
			(byte)RETURN
		}));
		put("StillLava", new BytecodeSignature(new byte[]{
			(byte)INVOKESTATIC, 0x00, 0x1E, // Math.random
			(byte)LDC2_W, 0x00, 0x13, // 0.005
			(byte)DCMPG,
			(byte)IFGE, 0x00, 0x10
		}));
		put("FlowLava", new BytecodeSignature(new byte[]{
			(byte)GETFIELD, 0x00, 0x1C,
			(byte)ICONST_3,
			(byte)IDIV,
			(byte)BIPUSH, 16,
			(byte)IMUL,
			(byte)ISUB,
			(byte)SIPUSH, 0x00, (byte)0xFF,
			(byte)IAND,
			(byte)FALOAD
		}));
		put("StillWater", new BytecodeSignature(new byte[]{
			(byte)INVOKESTATIC, 0x00, 0x19, // Math.random
			(byte)LDC2_W, 0x00, 0x0E, // 0.005
			(byte)DCMPG,
			(byte)IFGE, 0x00, 0x10
		}));
		put("FlowWater", new BytecodeSignature(new byte[]{
			(byte)GETFIELD, 0x00, 0x17,
			(byte)BIPUSH, 16,
			(byte)IMUL,
			(byte)ISUB,
			(byte)SIPUSH, 0x00, (byte)0xFF,
			(byte)IAND,
			(byte)FALOAD
		}));
		put("Tool3D", new ConstSignature(-0.9375F));
		put("Tessellator", new ConstSignature("Not tesselating!"));
		put("Minecraft", new FilenameSignature("net/minecraft/client/Minecraft.class"));
	}};
	private List<String> errors = new LinkedList<String>();

	public String getPath() {
		return file.getPath();
	}

	private interface ClassFinder {
		public boolean match(JarEntry entry, ClassFile cf) throws Exception;
	}

	private class MethodCallSignature implements ClassFinder {
		private String methodToFind;
		public MethodCallSignature(String methodToFind) {
			this.methodToFind = methodToFind;
		}
		public boolean match(JarEntry entry, ClassFile cf) {
			ConstPool cp = cf.getConstPool();
			for(int i = 1; i < cp.getSize(); ++i) {
				if( cp.getTag(i) == ConstPool.CONST_Methodref ) {
					String name = cp.getMethodrefName(i);
					if(name.equals(methodToFind))
						return true;
				}
			}
			return false;
		}
	}

	private class ConstSignature implements ClassFinder {
		Object constToFind;
		int tag;
		public ConstSignature(Object constToFind) {
			this.constToFind = constToFind;
			this.tag = ConstPoolUtils.getTag(constToFind);
		}
		public boolean match(JarEntry entry, ClassFile cf) {
			ConstPool cp = cf.getConstPool();
			for(int i = 1; i < cp.getSize(); ++i) {
				if( cp.getTag(i) == this.tag ) {
					if(ConstPoolUtils.checkEqual(cp, i, constToFind))
						return true;
				}
			}
			return false;
		}
	}

	private class BytecodeSignature implements ClassFinder {
		private String codeToMatch;
		public BytecodeSignature(byte[] codeToMatch) throws UnsupportedEncodingException {
			this.codeToMatch =  new String(codeToMatch, "ISO-8859-1");
		}
		public boolean match(JarEntry entry, ClassFile cf) throws UnsupportedEncodingException {
			for(Object mo : cf.getMethods()) {
				MethodInfo mi = (MethodInfo) mo;
				CodeAttribute ca = mi.getCodeAttribute();
				if(ca == null)
					continue;
				String methodCode = new String(ca.getCode(), "ISO-8859-1");
				if(methodCode.contains(this.codeToMatch))
					return true;
			}
			return false;
		}
	}

	private class FilenameSignature implements ClassFinder {
		String name;
		public FilenameSignature(String name) {
			this.name = name;
		}
		public boolean match(JarEntry entry, ClassFile cf) {
			return entry.getName().equals(this.name);
		}
	}

	public List<String> getErrors() {
		return errors;
	}

	public Minecraft(File jarFile) throws Exception {
		jar = new JarFile(jarFile, false);
		this.file = jarFile;
		for(JarEntry file : Collections.list(jar.entries())) {
			if(file.getName().endsWith(".class")) {
				ClassFile cf = new ClassFile(new DataInputStream(jar.getInputStream(file)));
				for(Map.Entry<String,ClassFinder> cfe : classFinders.entrySet()) {
					if(cfe.getValue().match(file, cf)) {
						if(classMap.containsKey(cfe.getKey())) {
							errors.add("Multiple classes match for " + cfe.getKey() + ": " +
								classMap.get(cfe.getKey()) + ", " + file.getName() );
						} else {
							classMap.put(cfe.getKey(), file.getName());
						}
					}
				}
			}
		}
		for(Map.Entry<String,ClassFinder> cfe : classFinders.entrySet()) {
			if(!classMap.containsKey(cfe.getKey())) {
				errors.add("No classes match for " + cfe.getKey());

			}
		}
	}

	public boolean isValid() {
		return (errors.size()==0);
	}
	
	public HashMap<String, String> getClassMap() {
		return classMap;
	}

	public boolean createBackup(File newFile) throws IOException {
		jar.close();
		if(file.renameTo(newFile)) {
			jar = new JarFile(newFile, false);
			return true;
		} else {
			jar = new JarFile(file, false);
			return false;
		}
	}
}
