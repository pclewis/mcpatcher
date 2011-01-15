import net.minecraft.client.Minecraft;

public class FlowWater extends AnimTexture {
	CustomAnimation anim;

	public FlowWater(Minecraft game) {
		super(12*16+13+1); // Block.waterMoving.blockIndexInTexture
		flow = 2;
		anim = new CustomAnimation(game, "water_flowing", tile, outBuf, 0, 0);
	}

	public void render() {
		anim.render();
	}
}
