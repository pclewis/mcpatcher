import net.minecraft.client.Minecraft;

public class fj extends ae {
	WaterAnimation w;

	public fj(Minecraft game) {
		super(og.C.bg + 1);
		this.e = 2;
		this.w = new WaterAnimation(game, "lava_flowing", this.b, this.a, 3, 6);
	}

	public void a() {
		this.w.a();
	}
}
