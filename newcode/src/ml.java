import net.minecraft.client.Minecraft;

public class ml extends z {
	WaterAnimation w;

	public ml(Minecraft game) {
		super(ly.B.bb);
		this.w = new WaterAnimation(game, "water_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}
