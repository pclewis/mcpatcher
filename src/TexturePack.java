import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class TexturePack {

	abstract protected DataInputStream openFile(String name) throws IOException;
	abstract public void close() throws IOException;

	private TexturePack parent;

	public String getPath() {
		return path;
	}

	private String path;
	private int terrainTileSize = -1;
	private int itemsTileSize = -1;

	public void setParent(TexturePack parent) {
		this.parent = parent;
	}

	static TexturePack open(String path, TexturePack parent) throws IOException {
		TexturePack result = null;
		if(path.endsWith(".jar")) {
			result = new JarTexturePack(path);
		} else if (path.endsWith(".zip")) {
			result = new ZipTexturePack(path);
		}
		if(result != null) {
			result.parent = parent;
			result.path = path;
		}
		return result;
	}

	public DataInputStream getInputStream(String file) {
		DataInputStream input = null;

		try {
			input = openFile(file);
			if(input==null)
				input = parent.openFile(file);
		} catch(IOException e) {
			throw new RuntimeException("IO error", e);
		}
		return input;
	}

	public boolean hasFile(String file) {
		DataInputStream input = getInputStream(file);
		if(input == null) {
			return false;
		}
		try {
			input.close();
		} catch (IOException e) {
		}
		return true;
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
		}
		return image.getWidth() / 16;
	}

	public int getTerrainTileSize() {
		if(terrainTileSize<=0)
			terrainTileSize = getTileSize("terrain.png");
		return terrainTileSize;
	}

	public int getItemsTileSize() {
		if(itemsTileSize<=0)
			itemsTileSize = getTileSize("gui/items.png");
		return itemsTileSize;
	}

	public String getFileSource(String file) {
		DataInputStream input = null;
		try {
			input = openFile(file);
			if(input != null)
				return this.path;
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
