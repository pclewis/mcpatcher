import net.minecraft.client.Minecraft;

public class nz extends ad {
	WaterAnimation w;

	public nz(Minecraft game) {
		super(nl.A.bg);
		this.w = new WaterAnimation(game, "water_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}
