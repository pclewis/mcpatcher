import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class at extends z {
	Minecraft game;
	int tileNumber;
	int tileWidth;
	int tileHeight;
	byte temp[];
	int timer = 0;
	private final static int MAX_TIMER=3;
	Random rand = new Random();

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
			if(rand.nextBoolean()) {
				if(rand.nextBoolean()) {
					System.arraycopy(this.a, (tileHeight-1)*tileWidth*4, this.temp, 0, tileWidth*4);
					System.arraycopy(this.a, 0, this.a, tileWidth*4, tileWidth*(tileHeight-1)*4);
					System.arraycopy(this.temp, 0, this.a, 0, tileWidth*4);
				} else {
					System.arraycopy(this.a, 0, this.temp, 0, tileWidth*4);
					System.arraycopy(this.a, tileWidth*4, this.a, 0, tileWidth*(tileHeight-1)*4);
					System.arraycopy(this.temp, 0, this.a, (tileHeight-1)*tileWidth*4, tileWidth*4);
				}
			} else {
				if(rand.nextBoolean()) { // left
					for(int y = 0; y < tileHeight; ++y) {
						int i = (y*tileWidth)*4;
						byte temp = this.a[i];
						System.arraycopy(this.a, i+4, this.a, i, (tileWidth-1)*4);
						this.a[i+(tileWidth-1)*4] = temp;
					}
				} else { // right
					for(int y = 0; y < tileHeight; ++y) {
						int i = (y*tileWidth)*4;
						byte temp = this.a[i+ (tileWidth-1)*4];
						System.arraycopy(this.a, i, this.a, i+4, (tileWidth-1)*4);
						this.a[i] = temp;
					}
				}
			}
		}
	}
}
