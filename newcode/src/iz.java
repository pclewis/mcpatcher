import net.minecraft.client.Minecraft;

public class iz extends ad {
	WaterAnimation w;

	public iz(Minecraft game) {
		super(nq.A.bg + 1);
		this.e = 2;
		w = new WaterAnimation(game, "water_flowing", this.b, this.a, 0, 0);
	}

	public void a() {
		this.w.a();
	}

}
