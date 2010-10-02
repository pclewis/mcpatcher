import de.innosystec.unrar.exception.RarException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class TexturePack {

	abstract public DataInputStream openFile(String name) throws IOException;

	private TexturePack parent;
	private String path;

	public void setParent(TexturePack parent) {
		this.parent = parent;
	}

	static TexturePack open(String path, TexturePack parent) throws IOException {
		TexturePack result = null;
		if(path.endsWith(".jar")) {
			result = new JarTexturePack(path);
		} else if (path.endsWith(".zip")) {
			result = new ZipTexturePack(path);
		} else if (path.endsWith(".rar")) {
			try {
				result = new RarTexturePack(path);
			} catch(RarException e) {
				throw new IOException("Rar Exception", e);
			}
		}
		if(result != null) {
			result.parent = parent;
			result.path = path;
		}
		return result;
	}

	private int getTileSize(String file) {
		BufferedImage image = null;
		DataInputStream input = null;

		try {
			input = openFile(file);
			if(input==null) {
				input = parent.openFile(file);
				if(input==null)
					return -1;
			}
			image = ImageIO.read(input);
		} catch(IOException e) {
			return -1;
		} finally {
			if(input!=null) {
				try {
					input.close();
				} catch(IOException e) {
				}
			}
		}
		return image.getWidth() / 16;
	}

	public int getTerrainTileSize() {
		return getTileSize("terrain.png");
	}

	public int getItemsTileSize() {
		return getTileSize("gui/items.png");
	}

	public String getFileSource(String file) {
		DataInputStream input = null;
		try {
			input = openFile(file);
			if(input != null) {
				input.close();
				return this.path;
			}
		} catch(IOException e) {
		}
		return parent!=null ? parent.getFileSource(file) : null;
	}

	public String getTerrainSource() {
		return getFileSource("terrain.png");
	}

	public String getItemsSource() {
		return getFileSource("gui/items.png");
	}
}
