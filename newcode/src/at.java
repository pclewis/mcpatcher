import net.minecraft.client.Minecraft;

public class at extends z {
	WaterAnimation w;

	public at(Minecraft game) {
		super(ly.D.bb);
		w = new WaterAnimation(game, "lava_still", this.b, this.a);
	}

	public void a() {
		this.w.a();
	}
}