import java.io.DataInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class JarTexturePack extends TexturePack {
	JarFile jar;

	public JarTexturePack(String path) throws IOException {
		super();
		jar = new JarFile(path, false);
	}

	public DataInputStream openFile(String name) throws IOException {
		ZipEntry e = jar.getEntry(name);
		if(e == null)
			return null;
		return new DataInputStream(jar.getInputStream(e));
	}
}
