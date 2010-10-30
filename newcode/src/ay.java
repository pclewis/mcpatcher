import net.minecraft.client.Minecraft;

public class ay extends ab {
	WaterAnimation w;

	public ay(Minecraft game) {
		super(ne.D.bh);
		w = new WaterAnimation(game, "lava_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}