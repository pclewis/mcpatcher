import java.util.Iterator;

public class Util {
	public static String joinString(Iterable<? extends Object> pColl, String separator) {
		Iterator<? extends Object> oIter;
		if(pColl == null || (!(oIter = pColl.iterator()).hasNext())) return "";
		StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
		while(oIter.hasNext()) oBuilder.append(separator).append(oIter.next());
		return oBuilder.toString();
	}
}
