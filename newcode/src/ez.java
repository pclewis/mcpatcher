import net.minecraft.client.Minecraft;

public class ez extends ad {
	WaterAnimation w;

	public ez(Minecraft game) {
		super(nq.C.bg + 1);
		this.e = 2;
		this.w = new WaterAnimation(game, "lava_flowing", this.b, this.a, 3, 6);
	}

	public void a() {
		this.w.a();
	}
}
