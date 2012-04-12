package net.minecraft.src;

import java.util.ArrayList;
import java.util.List;

public class TexturePackList {
    public void updateAvailableTexturePacks() {
    }

    public boolean setTexturePack(TexturePackBase texturePackBase) {
        return false;
    }

    public List<TexturePackBase> availableTexturePacks() {
        return new ArrayList<TexturePackBase>();
    }

    public TexturePackBase getDefaultTexturePack() { // added by BaseMod.TexturePackBaseMod
        return null;
    }

    public TexturePackBase getSelectedTexturePack() { // added by BaseMod.TexturePackBaseMod
        return null;
    }
}
