import java.util.HashMap;

public class Params {
	HashMap<String, String> map = new HashMap<String,String>();
	MissHandler missHandler;
	static public abstract class MissHandler {
		public abstract String get(String key);
	}

	public Params() {
		this(null);
	}

	public Params(MissHandler missHandler) {
		this.missHandler = missHandler;
	}

	public void put(String key, String value) {
		map.put(key, value);
	}

	public String get(String key) {
		if (map.containsKey(key))
			return map.get(key);
		if(missHandler != null)
			return missHandler.get(key);
		return null;
	}

	public int getInt(String key) {
		return Integer.parseInt(get(key), 10);
	}

	public byte getByte(String key) {
		return (byte)(getInt(key) & 0xFF);
	}
}
