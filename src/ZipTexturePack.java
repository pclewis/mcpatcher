import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipTexturePack extends TexturePack {
	ZipFile jar;

	public ZipTexturePack(String path) throws IOException {
		super();
		jar = new ZipFile(path);
	}

	public DataInputStream openFile(String name) throws IOException {
		ZipEntry e = jar.getEntry(name);
		if(e == null)
			return null;
		return new DataInputStream(jar.getInputStream(e));
	}
}
