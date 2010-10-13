import net.minecraft.client.Minecraft;

public class ht extends z {
	WaterAnimation w;

	public ht(Minecraft game) {
		super(ly.B.bb + 1);
		this.e = 2;
		w = new WaterAnimation(game, "water_flowing", this.b, this.a, 0, 0);
	}

	public void a() {
		this.w.a();
	}

}
