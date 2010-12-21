import net.minecraft.client.Minecraft;

public class jm extends ae {
	WaterAnimation w;

	public jm(Minecraft game) {
		super(of.A.bg + 1);
		this.e = 2;
		w = new WaterAnimation(game, "water_flowing", this.b, this.a, 0, 0);
	}

	public void a() {
		this.w.a();
	}

}
