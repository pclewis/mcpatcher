import net.minecraft.client.Minecraft;

public class bc extends ae {
	WaterAnimation w;

	public bc(Minecraft game) {
		super(of.C.bg);
		w = new WaterAnimation(game, "lava_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}