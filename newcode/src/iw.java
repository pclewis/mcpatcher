import net.minecraft.client.Minecraft;

public class iw extends ad {
	WaterAnimation w;

	public iw(Minecraft game) {
		super(nl.A.bg + 1);
		this.e = 2;
		w = new WaterAnimation(game, "water_flowing", this.b, this.a, 0, 0);
	}

	public void a() {
		this.w.a();
	}

}
