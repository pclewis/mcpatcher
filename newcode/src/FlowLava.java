import net.minecraft.client.Minecraft;

public class FlowLava extends AnimTexture {
	CustomAnimation anim;

	public FlowLava(Minecraft game) {
		super(14*16+13+1); // Block.lavaMoving.blockIndexInTexture
		flow = 2;
		anim = new CustomAnimation(game, "lava_flowing", tile, outBuf, 3, 6);
	}

	public void render() {
		anim.render();
	}
}
