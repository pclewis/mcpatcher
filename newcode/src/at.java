import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.*;
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
	boolean[] paletteDir;
	float[][] paletteHSB;
	int[] data;
	private static final float STEP = 0.01f;

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
			palette = new int[colors.length];
			paletteDir = new boolean[colors.length];
			paletteHSB = new float[colors.length][3];
			int p = 0;
			for(int c : colors) {
				palette[p] = c;
				getHSB(c, paletteHSB[p]);
				paletteDir[p] = (paletteHSB[p][2] > 0.5);
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
		for(int i = 0; i < palette.length; ++i) {
			float[] hsb = paletteHSB[i];
			if(paletteDir[i]) {
				float maxS = (hsb[1] < 0.5) ? 0.5F : 1.0F;
				float maxB = (hsb[2] < 0.5) ? 0.5F : 1.0F;
				//hsb[1] = Math.min(hsb[1] + STEP, maxS);
				hsb[2] = Math.min(hsb[2] + STEP, maxB);
				if(hsb[1] >= maxS || hsb[2] >= maxB)
					paletteDir[i] = false;
			} else {
				float minS = (hsb[1] < 0.5) ? 0.0F : 0.5F;
				float minB = (hsb[2] < 0.5) ? 0.0F : 0.5F;
				//hsb[1] = Math.max(hsb[1] - STEP, minS);
				hsb[2] = Math.max(hsb[2] - STEP, minB);
				if(hsb[1] <= minS || hsb[2] <= minB)
					paletteDir[i] = true;
			}
			//paletteHSB[i] = hsb;
			palette[i] = (palette[i] & 0xFF) | (Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) << 8);
		}
		for(int i = 0; i < data.length; ++i) {
			int v = palette[this.data[i]];
			this.a[(i*4)+0] = (byte)((v>>24)&0xFF);
			this.a[(i*4)+1] = (byte)((v>>16)&0xFF);
			this.a[(i*4)+2] = (byte)((v>> 8)&0xFF);
			this.a[(i*4)+3] = (byte)((v>> 0)&0xFF);
		}
	}

	public static int lightness(int v) {
		int r = (v>>24)&0xFF;
		int g = (v>>16)&0xFF;
		int b = (v>>8) &0xFF;
		return (Math.max(Math.max(r,g),b)+Math.min(Math.min(r,g),b))/2;
	}

	public static float[] getHSB(int v, float[] ar) {
		int r = (v>>24)&0xFF;
		int g = (v>>16)&0xFF;
		int b = (v>>8) &0xFF;
		return Color.RGBtoHSB(r,g,b,ar);
	}
}