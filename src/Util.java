import java.util.Iterator;

public class Util {

	public static final boolean mac;
	public static final boolean windows;
	public static final boolean linux;

	public static boolean isMac() { return mac; }
	public static boolean isWindows() { return windows; }
	public static boolean isLinux() { return linux; }

	static {
		String os = System.getProperty("os.name").toLowerCase();
		mac       = os != null && os.equals("mac os x");
		windows   = os != null && os.indexOf("windows") != -1;
		linux     = os != null && os.indexOf("linux") != -1;
	}

	public static String joinString(Iterable<? extends Object> pColl, String separator) {
		Iterator<? extends Object> oIter;
		if(pColl == null || (!(oIter = pColl.iterator()).hasNext())) return "";
		StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
		while(oIter.hasNext()) oBuilder.append(separator).append(oIter.next());
		return oBuilder.toString();
	}
}
