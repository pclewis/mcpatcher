import net.minecraft.client.Minecraft;

public class nr extends ab {
	WaterAnimation w;

	public nr(Minecraft game) {
		super(ne.B.bh);
		this.w = new WaterAnimation(game, "water_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}
