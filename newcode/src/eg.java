import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class eg extends z {
	Minecraft game;
	int tileNumber;
	int tileWidth;
	int tileHeight;
	byte temp[];
	int timer = 0;
	private final static int MAX_TIMER=3;

	public eg(Minecraft game) {
		super(ly.D.bb + 1);
		this.e = 2;
		this.game = game;
		this.tileNumber = this.b;
		try {
			BufferedImage tiles = ImageIO.read(Minecraft.class.getResource("/terrain.png"));
			tileWidth = tiles.getWidth() / 16;
			tileHeight = tiles.getHeight() / 16;
			int tileX = tileNumber % 16 * tileWidth;
			int tileY = tileNumber / 16 * tileHeight;
			int imageBuf[] = new int[tileWidth * tileHeight];
			tiles.getRGB(tileX, tileY, tileWidth, tileHeight, imageBuf, 0, tileWidth);
			for( int i = 0; i < imageBuf.length; ++i ) {
				int v = imageBuf[i];
				//ARGB -> RGBA
				//System.out.println(String.format("%08x -> %02x", v, (byte)((v>>24)&0xFF)));
				this.a[(i*4)+3] = (byte)((v>>24)&0xFF);
				this.a[(i*4)+0] = (byte)((v>>16)&0xFF);
				this.a[(i*4)+1] = (byte)((v>> 8)&0xFF);
				this.a[(i*4)+2] = (byte)((v>> 0)&0xFF);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}

		temp = new byte[tileWidth*4];
	}

	public void a() {
		if(++timer >= MAX_TIMER) {
			timer = 0;
			System.arraycopy(this.a, (tileHeight-1)*tileWidth*4, this.temp, 0, tileWidth*4);
			System.arraycopy(this.a, 0, this.a, tileWidth*4, tileWidth*(tileHeight-1)*4);
			System.arraycopy(this.temp, 0, this.a, 0, tileWidth*4);
		}
	}
}
