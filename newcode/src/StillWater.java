import net.minecraft.client.Minecraft;

public class StillWater extends AnimTexture {
	CustomAnimation anim;

	public StillWater(Minecraft game) {
		super(12*16+13); // Block.waterStill.blockIndexInTexture
		anim = new CustomAnimation(game, "water_still", tile, outBuf);
	}

	public void render() {
		anim.render();
	}
}
