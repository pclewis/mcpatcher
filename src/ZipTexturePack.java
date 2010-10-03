import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipTexturePack extends TexturePack {
	protected ZipFile jar;

	public ZipTexturePack(String path) throws IOException {
		super();
		jar = new ZipFile(path);
	}

	public ZipTexturePack(ZipFile jar) throws IOException {
		this.jar = jar;
	}

	protected DataInputStream openFile(String name) throws IOException {
		ZipEntry e = jar.getEntry(name);
		if(e == null) {
			for(ZipEntry e2 : Collections.list(jar.entries())) {
				if(e2.getName().endsWith(name)) {
					e = e2;
					break;
				}
			}
			if(e==null)
				return null;
		}
		return new DataInputStream(jar.getInputStream(e));
	}

	public void close() throws IOException {
		jar.close();
	}
}
