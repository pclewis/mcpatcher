import net.minecraft.client.Minecraft;

public class fi extends ae {
	WaterAnimation w;

	public fi(Minecraft game) {
		super(of.C.bg + 1);
		this.e = 2;
		this.w = new WaterAnimation(game, "lava_flowing", this.b, this.a, 3, 6);
	}

	public void a() {
		this.w.a();
	}
}
