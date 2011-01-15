import net.minecraft.client.Minecraft;

public class Portal extends AnimTexture {
	CustomAnimation anim;

	public Portal(Minecraft game) {
		super(14); // Block.portal.blockIndexInTexture
		anim = new CustomAnimation(game, "portal", tile, outBuf);
	}

	public void render() {
		anim.render();
	}
}