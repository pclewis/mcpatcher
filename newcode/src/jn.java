import net.minecraft.client.Minecraft;

public class jn extends ae {
	WaterAnimation w;

	public jn(Minecraft game) {
		super(og.A.bg + 1);
		this.e = 2;
		w = new WaterAnimation(game, "water_flowing", this.b, this.a, 0, 0);
	}

	public void a() {
		this.w.a();
	}

}
