import net.minecraft.client.Minecraft;

public class StillLava extends AnimTexture {
	CustomAnimation anim;

	public StillLava(Minecraft game) {
		super(14*16+13); // Block.lavaStill.blockIndexInTexture
		anim = new CustomAnimation(game, "lava_still", tile, outBuf);
	}

	public void render() {
		anim.render();
	}
}