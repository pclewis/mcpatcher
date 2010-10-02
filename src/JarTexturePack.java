import java.io.DataInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class JarTexturePack extends ZipTexturePack {
	public JarTexturePack(String path) throws IOException {
		super(new JarFile(path, false));
	}
}
