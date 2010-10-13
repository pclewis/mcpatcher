import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class eg extends z {
	WaterAnimation w;

	public eg(Minecraft game) {
		super(ly.D.bb + 1);
		this.e = 2;
		this.w = new WaterAnimation(game, "lava_flowing", this.b, this.a, 3, 6);
	}

	public void a() {
		this.w.a();
	}
}
