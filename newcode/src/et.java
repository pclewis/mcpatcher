import net.minecraft.client.Minecraft;

public class et extends ab {
	WaterAnimation w;

	public et(Minecraft game) {
		super(ne.D.bh + 1);
		this.e = 2;
		this.w = new WaterAnimation(game, "lava_flowing", this.b, this.a, 3, 6);
	}

	public void a() {
		this.w.a();
	}
}
