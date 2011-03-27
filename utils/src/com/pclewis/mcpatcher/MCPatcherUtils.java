import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Collection of static methods available to mods at runtime.  This class is always injected into
 * the output minecraft jar.
 */
public class MCPatcherUtils {
    private static File propFile = null;
    private static Properties properties = new Properties();

    private MCPatcherUtils() {
    }

    static {
        String os = System.getProperty("os.name").toLowerCase();
        String dir1 = null;
        String dir2 = ".minecraft";
        if (os.contains("win")) {
            dir1 = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            dir1 = System.getProperty("user.home");
            if (dir1 != null) {
                dir1 = new File(dir1, "Library/Application Support").getPath();
            }
            dir2 = "minecraft";
        }
        if (dir1 == null) {
            dir1 = System.getProperty("user.home");
        }

        File mcDir = new File(dir1, dir2);
        if (mcDir.exists()) {
            propFile = new File(mcDir, "mcpatcher.properties");

            try {
                if (propFile.exists()) {
                    properties.load(new FileInputStream(propFile));
                    System.out.printf("read %s\n", propFile.getPath());
                } else {
                    set("HDTexture", "enableAnimations", true);
                    set("HDTexture", "useCustomAnimations", true);
                    saveProperties();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getPropertyKey(String mod, String name) {
        if (mod == null || mod.equals("")) {
            return name;
        } else {
            return mod + "." + name;
        }
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param mod name of mod
     * @param name property name
     * @return String value
     */
    public static String getString(String mod, String name) {
        return properties.getProperty(getPropertyKey(mod, name));
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param name property name
     * @return String value
     */
    public static String getString(String name) {
        return getString(null, name);
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param mod name of mod
     * @param name property name
     * @return int value or 0
     */
    public static int getInt(String mod, String name) {
        int value = 0;
        try {
            value = Integer.parseInt(getString(mod, name));
        } catch (NumberFormatException e) {
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param name property name
     * @return int value or 0
     */
    public static int getInt(String name) {
        return getInt(null, name);
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param mod name of mod
     * @param name property name
     * @return boolean value
     */
    public static boolean getBoolean(String mod, String name) {
        return Boolean.parseBoolean(getString(mod, name));
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param name property name
     * @return boolean value
     */
    public static boolean getBoolean(String name) {
        return getBoolean(null, name);
    }

    /**
     * Sets a value in mcpatcher.properties.
     *
     * @param mod name of mod
     * @param name property name
     * @param value property value (must support toString())
     */
    public static void set(String mod, String name, Object value) {
        properties.setProperty(getPropertyKey(mod, name), value.toString());
    }

    /**
     * Save all properties to mcpatcher.properties.

     * @return true if successful
     */
    public static boolean saveProperties() {
        boolean saved = false;
        FileOutputStream os = null;
        if (properties != null && propFile != null) {
            try {
                os = new FileOutputStream(propFile);
                properties.store(os, "settings for MCPatcher");
                saved = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return saved;
    }
}
