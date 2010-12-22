import net.minecraft.client.Minecraft;

public class ou extends ae {
	WaterAnimation w;

	public ou(Minecraft game) {
		super(og.A.bg);
		this.w = new WaterAnimation(game, "water_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}
