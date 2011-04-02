import java.io.InputStream;

abstract public class TexturePackBase {
    public String texturePackFileName;
    abstract public InputStream getInputStream(String s);
}
