import de.innosystec.unrar.exception.RarException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class TexturePack {

	abstract public DataInputStream openFile(String name) throws IOException;

	static TexturePack open(String path) throws IOException {
		if(path.endsWith(".jar")) {
			return new JarTexturePack(path);
		} else if (path.endsWith(".zip")) {
			return new ZipTexturePack(path);
		} else if (path.endsWith(".rar")) {
			try {
				return new RarTexturePack(path);
			} catch(RarException e) {
				throw new IOException("Rar Exception", e);
			}
		}
		return null;
	}

	private int getTileSize(String file) {
		BufferedImage image;
		try {
			image = ImageIO.read(openFile(file));
		} catch(IOException e) {
			return -1;
		}
		return image.getWidth() / 16;
	}

	public int getTerrainTileSize() {
		return getTileSize("terrain.png");
	}
}
