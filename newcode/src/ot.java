import net.minecraft.client.Minecraft;

public class ot extends ae {
	WaterAnimation w;

	public ot(Minecraft game) {
		super(of.A.bg);
		this.w = new WaterAnimation(game, "water_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}
