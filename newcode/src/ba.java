import net.minecraft.client.Minecraft;

public class ba extends ad {
	WaterAnimation w;

	public ba(Minecraft game) {
		super(nl.C.bg);
		w = new WaterAnimation(game, "lava_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}