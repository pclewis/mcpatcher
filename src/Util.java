import java.io.*;
import java.util.Iterator;

public class Util {

	public static final boolean mac;
	public static final boolean windows;
	public static final boolean linux;
	public static final boolean is64bit;

	public static boolean isMac() { return mac; }
	public static boolean isWindows() { return windows; }
	public static boolean isLinux() { return linux; }
	public static boolean is64Bit() { return is64bit; }

	static {
		String os = System.getProperty("os.name").toLowerCase();
		mac       = os != null && os.equals("mac os x");
		windows   = os != null && os.indexOf("windows") != -1;
		linux     = os != null && os.indexOf("linux") != -1;

		String datamodel = System.getProperty("sun.arch.data.model");  // sun-specific, but gets the arch of the jvm
		String arch = System.getProperty("os.arch");  // generic, but gets the arch of the os, not the jvm (may be a 32-bit jvm on a 64-bit os)
		if(datamodel != null) {
			is64bit = (Integer.parseInt(datamodel) >= 64);
		} else if (arch != null) {
			is64bit = arch.contains("64");
		} else {
			is64bit = false;
		}
	}

	public static String joinString(Iterable<? extends Object> pColl, String separator) {
		Iterator<? extends Object> oIter;
		if(pColl == null || (!(oIter = pColl.iterator()).hasNext())) return "";
		StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
		while(oIter.hasNext()) oBuilder.append(separator).append(oIter.next());
		return oBuilder.toString();
	}

	protected static byte b(int value, int index) {
	    return (byte)((value >> (index*8)) & 0xFF);
	}

	static void copyStream(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024];
		while(true) {
			int count = input.read(buffer);
			if(count == -1)
				break;
			output.write(buffer, 0, count);
		}
	}

	static void copyFile(File input, File output) throws IOException {
		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(input);
			out = new FileOutputStream(output);
			Util.copyStream(in, out);
		} catch(IOException e) {
			throw e;
		} finally {
			try { if (in != null) in.close(); } catch(IOException e) { }
			try { if (out != null) out.close(); } catch(IOException e) { }
		}
	}
}
