package net.minecraft.src;

import java.util.ArrayList;
import java.util.List;

public class TexturePackList {
    public TexturePackBase defaultTexturePack;
    public TexturePackBase selectedTexturePack;

    public void updateAvailableTexturePacks() {
    }

    public boolean setTexturePack(TexturePackBase texturePackBase) {
        return false;
    }

    public List<TexturePackBase> availableTexturePacks() {
        return new ArrayList<TexturePackBase>();
    }
}
