package com.pclewis.mcpatcher;

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
    private static File minecraftDir;
    private static File propFile = null;
    private static Properties properties = new Properties();
    private static boolean needSaveProps = false;
    private static boolean debug;

    private MCPatcherUtils() {
    }

    static {
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

        minecraftDir = new File(baseDir, subDir);
        if (minecraftDir.exists()) {
            propFile = new File(minecraftDir, "mcpatcher.properties");

            try {
                if (propFile.exists()) {
                    properties.load(new FileInputStream(propFile));
                    remove("HDTexture.enableAnimations");
                    remove("HDTexture.useCustomAnimations");
                    remove("HDTexture.glBufferSize");
                    // remove("debug"); TODO: uncomment when out of beta
                } else {
                    needSaveProps = true;
                }
                debug = getBoolean("debug", true); // TODO: change to false when out of beta
                saveProperties();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
     * @param mod          name of mod
     * @param name         property name
     * @param defaultValue default value if not found in .properties file
     * @return String value
     */
    public static String getString(String mod, String name, Object defaultValue) {
        String key = getPropertyKey(mod, name);
        String value = properties.getProperty(key);
        if (value == null && defaultValue != null) {
            value = defaultValue.toString();
            if (!value.equals("")) {
                properties.setProperty(key, value);
                needSaveProps = true;
            }
        }
        return value == null ? "" : value;
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param name         property name
     * @param defaultValue default value if not found in .properties file
     * @return String value
     */
    public static String getString(String name, Object defaultValue) {
        return getString(null, name, defaultValue);
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param mod          name of mod
     * @param name         property name
     * @param defaultValue default value if not found in .properties file
     * @return int value or 0
     */
    public static int getInt(String mod, String name, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(getString(mod, name, Integer.valueOf(defaultValue).toString()));
        } catch (NumberFormatException e) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param name         property name
     * @param defaultValue default value if not found in .properties file
     * @return int value or 0
     */
    public static int getInt(String name, int defaultValue) {
        return getInt(null, name, defaultValue);
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param mod          name of mod
     * @param name         property name
     * @param defaultValue default value if not found in .properties file
     * @return boolean value
     */
    public static boolean getBoolean(String mod, String name, boolean defaultValue) {
        String value = getString(mod, name, Boolean.valueOf(defaultValue).toString()).toLowerCase();
        if (value.equals("false")) {
            return false;
        } else if (value.equals("true")) {
            return true;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets a value from mcpatcher.properties.
     *
     * @param name         property name
     * @param defaultValue default value if not found in .properties file
     * @return boolean value
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        return getBoolean(null, name, defaultValue);
    }

    /**
     * Sets a value in mcpatcher.properties.
     *
     * @param mod   name of mod
     * @param name  property name
     * @param value property value (must support toString())
     */
    public static void set(String mod, String name, Object value) {
        properties.setProperty(getPropertyKey(mod, name), value.toString());
        needSaveProps = true;
    }

    static void set(String name, Object value) {
        set(null, name, value);
    }

    /**
     * Remove a value from mcpatcher.properties.
     *
     * @param mod  name of mod
     * @param name property name
     */
    public static void remove(String mod, String name) {
        String key = getPropertyKey(mod, name);
        if (properties.containsKey(key)) {
            properties.remove(key);
            needSaveProps = true;
        }
    }

    static void remove(String name) {
        remove(null, name);
    }

    /**
     * Save all properties to mcpatcher.properties.
     *
     * @return true if successful
     */
    public static boolean saveProperties() {
        if (!needSaveProps) {
            return true;
        }
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
        needSaveProps = false;
        return saved;
    }
}
