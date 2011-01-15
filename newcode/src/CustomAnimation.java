import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class CustomAnimation {
	Minecraft game;
	int tileNumber;
	int tileWidth;
	int tileHeight;
	int frame;
	int numFrames;
	byte[] src;
	byte[] outBuf;
	byte[] temp;
	int minScrollDelay = -1;
	int maxScrollDelay = -1;
	int timer = -1;
	boolean isScrolling;
	boolean isCustom;
	static private Random rand = new Random();

	public CustomAnimation(Minecraft game, String name, int tileNumber, byte[] outBuf) {
		this(game, name, tileNumber, outBuf, -1, -1);
	}

	public CustomAnimation(Minecraft game, String name, int tileNumber, byte[] outBuf, int minScrollDelay, int maxScrollDelay) {
		this.game           = game;
		this.tileNumber     = tileNumber;
		this.outBuf         = outBuf;
		this.minScrollDelay = minScrollDelay;
		this.maxScrollDelay = maxScrollDelay;
		this.isScrolling    = (minScrollDelay >= 0);

		BufferedImage tiles;
		try {
			tiles = ImageIO.read(Minecraft.class.getResource("/terrain.png"));
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		tileWidth  = tiles.getWidth()  / 16;
		tileHeight = tiles.getHeight() / 16;

		BufferedImage custom = null;
		try {
			URL customUrl = Minecraft.class.getResource("/custom_"+name+".png");
			if(customUrl != null)
				custom = ImageIO.read(customUrl);
		} catch (IOException ex) {
			custom = null;
		}

		if(custom != null) {
			numFrames = custom.getHeight()/custom.getWidth();
			if(custom.getWidth() != tileWidth) {
				BufferedImage newImage = new BufferedImage(tileWidth, tileHeight * numFrames, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics2D = newImage.createGraphics();
				graphics2D.drawImage(custom, 0, 0, tileWidth, tileHeight * numFrames, null);
				custom = newImage;
			}
			int imageBuf[] = new int[custom.getWidth() * custom.getHeight()];
			custom.getRGB(0, 0, custom.getWidth(), custom.getHeight(), imageBuf, 0, tileWidth);
			src = new byte[imageBuf.length * 4];
			ARGBtoRGBA(imageBuf, src);
			isCustom = true;
		} else {
			int tileX = tileNumber % 16 * tileWidth;
			int tileY = tileNumber / 16 * tileHeight;
			int imageBuf[] = new int[tileWidth * tileHeight];
			tiles.getRGB(tileX, tileY, tileWidth, tileHeight, imageBuf, 0, tileWidth);
			ARGBtoRGBA(imageBuf, outBuf);
			if(isScrolling) {
				temp = new byte[tileWidth*4];
			}
		}
	}

	private void ARGBtoRGBA(int[] src, byte[] dest) {
		for(int i = 0; i < src.length; ++i) {
			int v = src[i];
			dest[(i * 4) + 3] = (byte) ((v >> 24) & 0xFF);
			dest[(i * 4) + 0] = (byte) ((v >> 16) & 0xFF);
			dest[(i * 4) + 1] = (byte) ((v >>  8) & 0xFF);
			dest[(i * 4) + 2] = (byte) ((v >>  0) & 0xFF);
		}
	}

	public void render() {
		if(this.src != null) {
			if(++this.frame >= numFrames)
				this.frame = 0;
			System.arraycopy(this.src, (this.frame * (tileHeight * tileWidth*4)), outBuf, 0, tileHeight * tileWidth*4);
		} else if (isScrolling) {
			if(maxScrollDelay <= 0 || --this.timer <= 0) {
				if(maxScrollDelay > 0)
					timer = rand.nextInt(maxScrollDelay-minScrollDelay+1) + minScrollDelay;
				System.arraycopy(this.outBuf, (tileHeight-1)*tileWidth*4, this.temp, 0, tileWidth*4);
				System.arraycopy(this.outBuf, 0, this.outBuf, tileWidth*4, tileWidth*(tileHeight-1)*4);
				System.arraycopy(this.temp, 0, this.outBuf, 0, tileWidth*4);
			}
		}
	}

}
