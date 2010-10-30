import net.minecraft.client.Minecraft;

public class iq extends ab {
	WaterAnimation w;

	public iq(Minecraft game) {
		super(ne.B.bh + 1);
		this.e = 2;
		w = new WaterAnimation(game, "water_flowing", this.b, this.a, 0, 0);
	}

	public void a() {
		this.w.a();
	}

}
