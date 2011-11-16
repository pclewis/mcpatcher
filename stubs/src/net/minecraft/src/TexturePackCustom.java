package net.minecraft.src;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class TexturePackCustom extends TexturePackBase {
    public ZipFile zipFile;
    public File file;

    public File tmpFile;
    public ZipFile origZip;
    public long lastModified;
}
