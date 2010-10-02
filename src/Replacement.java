import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Replacement {
	public byte[] from;
	public byte[] to;
	public String description;

	public Replacement(String description, byte[] from, byte[] to) {
		this.description = description;
		this.from = from;
		this.to = to;
	}

	public Replacement(String description, int[] from, int[] to) {
		this.description = description;
		this.from = intsToBytes(from);
		this.to = intsToBytes(to);
	}

	/* Careful with this, something like SIPUSH 16 won't do what you want */
	private byte[] intsToBytes(int[] src) {
		ByteBuffer bb = ByteBuffer.allocate(src.length * 4);
		for(int f : src) {
			if((f & 0xFF000000) != 0) bb.put((byte) ((f >> 24) & 0xFF));
			if((f & 0x00FF0000) != 0) bb.put((byte) ((f >> 16) & 0xFF));
			if((f & 0x0000FF00) != 0) bb.put((byte) ((f >> 8) & 0xFF));
			bb.put((byte) (f & 0xFF));
		}
		byte[] r = new byte[bb.position()];
		bb.flip();
		bb.get(r);
		return r;
	}
}
