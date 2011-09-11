package com.pclewis.mcpatcher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Collection of static methods available to mods at runtime.  This class is always injected into
 * the output minecraft jar.
 */
public class MCPatcherUtils {
    private static File minecraftDir = null;
    private static boolean debug = false;
    private static boolean isGame;
    static Config config = null;

    public static final String HD_TEXTURES = "HD Textures";
    public static final String HD_FONT = "HD Font";
    public static final String BETTER_GRASS = "Better Grass";
    public static final String ONE_EIGHT = "1.8 Bug Fixes";
    public static final String GLSL_SHADERS = "GLSL Shaders";

    private MCPatcherUtils() {
    }

    static {
        isGame = true;
        try {
            if (Class.forName("com.pclewis.mcpatcher.MCPatcher") != null) {
                isGame = false;
            }
        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (isGame) {
            if (setGameDir(new File(".")) || setGameDir(getDefaultGameDir())) {
                System.out.println("MCPatcherUtils initialized. Directory " + minecraftDir.getPath());
            } else {
                System.out.println("MCPatcherUtils initialized. Current directory " + new File(".").getAbsolutePath());
            }
        }
    }

    static File getDefaultGameDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String baseDir = null;
        String subDir = ".minecraft";
        if (os.contains("win")) {
            baseDir = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            subDir = "Library/Application Support/minecraft";
        }
        if (baseDir == null) {
            baseDir = System.getProperty("user.home");
        }
        return new File(baseDir, subDir);
    }

    static boolean setGameDir(File dir) {
        if (dir != null &&
            dir.isDirectory() &&
            new File(dir, "bin/lwjgl.jar").exists() &&
            new File(dir, "resources").isDirectory()) {
            minecraftDir = dir.getAbsoluteFile();
        } else {
            minecraftDir = null;
        }
        return loadProperties();
    }

    private static boolean loadProperties() {
        config = null;
        if (minecraftDir != null && minecraftDir.exists()) {
            try {
                config = new Config(minecraftDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
            debug = getBoolean(Config.TAG_DEBUG, false);
            return true;
        }
        return false;
    }

    /**
     * Get the path to a file/directory within the minecraft folder.
     *
     * @param subdirs zero or more path components
     * @return combined path
     */
    public static File getMinecraftPath(String... subdirs) {
        File f = minecraftDir;
        for (String s : subdirs) {
            f = new File(f, s);
        }
        return f;
    }

    /**
     * Write a debug message to minecraft standard output.
     *
     * @param format printf-style format string
     * @param params printf-style parameters
     */
    public static void log(String format, Object... params) {
        if (debug) {
            System.out.printf(format + "\n", params);
        }
    }

    /**
     * Returns true if running inside game, false if running inside MCPatcher.  Useful for
     * code shared between mods and runtime classes.
     *
     * @return true if in game
     */
    public static boolean isGame() {
        return isGame;
    }

    /**
     * Write a warning message to minecraft standard output.
     *
     * @param format printf-style format string
     * @param params printf-style parameters
     */
    public static void warn(String format, Object... params) {
        System.out.printf("WARNING: " + format + "\n", params);
    }

    /**
     * Write an error message to minecraft standard output.
     *
     * @param format printf-style format string
     * @param params printf-style parameters
     */
    public static void error(String format, Object... params) {
        System.out.printf("ERROR: " + format + "\n", params);
    }

    /**
     * Gets a value from mcpatcher.xml.
     *
     * @param mod          name of mod
     * @param tag          property name
     * @param defaultValue default value if not found in .properties file
     * @return String value
     */
    public static String getString(String mod, String tag, Object defaultValue) {
        if (config == null) {
            return defaultValue == null ? null : defaultValue.toString();
        }
        String value = config.getModConfigValue(mod, tag);
        if (value == null && defaultValue != null) {
            value = defaultValue.toString();
            config.setModConfigValue(mod, tag, value);
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.xml.
     *
     * @param tag          property name
     * @param defaultValue default value if not found in .properties file
     * @return String value
     */
    public static String getString(String tag, Object defaultValue) {
        if (config == null) {
            return defaultValue == null ? null : defaultValue.toString();
        }
        String value = config.getConfigValue(tag);
        if (value == null && defaultValue != null) {
            value = defaultValue.toString();
            config.setConfigValue(tag, value);
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.xml.
     *
     * @param mod          name of mod
     * @param tag          property name
     * @param defaultValue default value if not found in .properties file
     * @return int value or 0
     */
    public static int getInt(String mod, String tag, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(getString(mod, tag, defaultValue));
        } catch (NumberFormatException e) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.xml.
     *
     * @param tag          property name
     * @param defaultValue default value if not found in .properties file
     * @return int value or 0
     */
    public static int getInt(String tag, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(getString(tag, defaultValue));
        } catch (NumberFormatException e) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.xml.
     *
     * @param mod          name of mod
     * @param tag          property name
     * @param defaultValue default value if not found in .properties file
     * @return boolean value
     */
    public static boolean getBoolean(String mod, String tag, boolean defaultValue) {
        String value = getString(mod, tag, defaultValue).toLowerCase();
        if (value.equals("false")) {
            return false;
        } else if (value.equals("true")) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets a value from mcpatcher.xml.
     *
     * @param tag          property name
     * @param defaultValue default value if not found in .properties file
     * @return boolean value
     */
    public static boolean getBoolean(String tag, boolean defaultValue) {
        String value = getString(tag, defaultValue).toLowerCase();
        if (value.equals("false")) {
            return false;
        } else if (value.equals("true")) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * Sets a value in mcpatcher.xml.
     *
     * @param mod   name of mod
     * @param tag   property name
     * @param value property value (must support toString())
     */
    public static void set(String mod, String tag, Object value) {
        if (config != null) {
            config.setModConfigValue(mod, tag, value.toString());
        }
    }

    /**
     * Set a global config value in mcpatcher.xml.
     *
     * @param tag   property name
     * @param value property value (must support toString())
     */
    static void set(String tag, Object value) {
        if (config != null) {
            config.setConfigValue(tag, value.toString());
        }
    }

    /**
     * Remove a value from mcpatcher.xml.
     *
     * @param mod name of mod
     * @param tag property name
     */
    public static void remove(String mod, String tag) {
        if (config != null) {
            config.remove(config.getModConfig(mod, tag));
        }
    }

    /**
     * Remove a global config value from mcpatcher.xml.
     *
     * @param tag property name
     */
    static void remove(String tag) {
        if (config != null) {
            config.remove(config.getConfig(tag));
        }
    }

    /**
     * Convenience method to close a stream ignoring exceptions.
     *
     * @param closeable closeable object
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Convenience method to close a zip file ignoring exceptions.
     *
     * @param zip zip file
     */
    public static void close(ZipFile zip) {
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
