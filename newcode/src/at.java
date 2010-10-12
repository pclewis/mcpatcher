import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class at extends z {
	Minecraft game;
	int tileNumber;
	int tileWidth;
	int tileHeight;
	int[] palette;
	int[] data;

	public at(Minecraft game) {
		super(ly.D.bb);

		this.game = game;
		this.tileNumber = this.b;
		try {
			BufferedImage tiles = ImageIO.read(Minecraft.class.getResource("/terrain.png"));
			tileWidth = tiles.getWidth() / 16;
			tileHeight = tiles.getHeight() / 16;
			int tileX = tileNumber % 16 * tileWidth;
			int tileY = tileNumber / 16 * tileHeight;
			int imageBuf[] = new int[tileWidth * tileHeight];
			data = new int[tileWidth * tileHeight];
			tiles.getRGB(tileX, tileY, tileWidth, tileHeight, imageBuf, 0, tileWidth);
			HashMap<Integer,Integer> palBuilder = new HashMap<Integer,Integer>();

			for(int i = 0; i < imageBuf.length; ++i) {
				int v = imageBuf[i];
				int c = ((v>>24)&0xFF) | ((v &0x00FFFFFF)<<8);
				if(!palBuilder.containsKey(c)) {
					palBuilder.put(c, 0);
				}
			}
			Integer[] colors = new Integer[palBuilder.size()];
			palBuilder.keySet().toArray(colors);
			Arrays.sort(colors);
			palette = new int[colors.length];
			int p = 0;
			for(int c : colors) {
				palette[p] = c;
				palBuilder.put(c, p++);
			}
			for(int i = 0; i < imageBuf.length; ++i) {
				int v = imageBuf[i];
				int c = ((v>>24)&0xFF) | ((v &0x00FFFFFF)<<8);
				data[i] = palBuilder.get(c);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void a() {
		int temp = palette[palette.length-1];
		System.arraycopy(palette, 0, palette, 1, palette.length-1);
		palette[0] = temp;
		for(int i = 0; i < data.length; ++i) {
			int v = palette[this.data[i]];
			this.a[(i*4)+0] = (byte)((v>>24)&0xFF);
			this.a[(i*4)+1] = (byte)((v>>16)&0xFF);
			this.a[(i*4)+2] = (byte)((v>> 8)&0xFF);
			this.a[(i*4)+3] = (byte)((v>> 0)&0xFF);
		}
	}
}