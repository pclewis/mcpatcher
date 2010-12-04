import net.minecraft.client.Minecraft;

public class oe extends ad {
	WaterAnimation w;

	public oe(Minecraft game) {
		super(nq.A.bg);
		this.w = new WaterAnimation(game, "water_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}
