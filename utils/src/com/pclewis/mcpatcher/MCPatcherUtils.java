package com.pclewis.mcpatcher;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
    private static File minecraftDir = null;
    private static File propFile = null;
    private static File xmlFile = null;
    private static Document xml;
    private static Properties properties;
    private static boolean needSaveProps = false;
    private static boolean debug = false;
    private static boolean isGame;

    private MCPatcherUtils() {
    }

    static {
        properties = new Properties();
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
        propFile = null;
        properties = new Properties();
        needSaveProps = false;
        if (minecraftDir != null && minecraftDir.exists()) {
            propFile = new File(minecraftDir, "mcpatcher.properties");
            if (propFile.exists()) {
                try {
                    properties.load(new FileInputStream(propFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                remove("HDTexture", "enableAnimations");
                remove("HDTexture", "useCustomAnimations");
                remove("HDTexture", "glBufferSize");
            } else {
                needSaveProps = true;
            }
            debug = getBoolean("debug", false);
            saveProperties();

            xmlFile = new File(minecraftDir, "mcpatcher.xml");
            DocumentBuilder builder = null;
            xml = null;
            if (xmlFile.exists()) {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    builder = factory.newDocumentBuilder();
                    xml = builder.parse(xmlFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (builder != null && xml == null) {
                xml = builder.newDocument();
                buildNewProperties();
            }

            return true;
        }
        return false;
    }

    /*
     * <mcpatcher-profile>
     *     <config>
     *         <debug>false</debug>
     *         <java-heap-size>1024</java-heap-size>
     *         <last-version>2.0.1</last-version>
     *         <beta-warning-shown>false</beta-warning-shown>
     *     </config>
     *     <mods>
     *         <mod>
     *             <name>HD Textures</name>
     *             <type>builtin</type>
     *             <enabled>true</enabled>
     *             <config>
     *                 <useAnimatedTextures>true</useAnimatedTextures>
     *             </config>
     *         </mod>
     *         <mod>
     *             <name>ModLoader</name>
     *             <type>external zip</type>
     *             <path>/home/user/.minecraft/mods/ModLoader.zip</path>
     *             <prefix />
     *             <enabled>true</enabled>
     *         </mod>
     *     </mods>
     * </mcpatcher-profile>
     */
    static final String TAG_ROOT = "mcpatcher-profile";
    static final String TAG_CONFIG1 = "config";
    static final String TAG_DEBUG = "debug";
    static final String TAG_JAVA_HEAP_SIZE = "java-heap-size";
    static final String TAG_LAST_VERSION = "last-version";
    static final String TAG_BETA_WARNING_SHOWN = "beta-warning-shown";
    static final String TAG_MODS = "mods";
    static final String TAG_MOD = "mod";
    static final String TAG_NAME = "name";
    static final String TAG_TYPE = "type";
    static final String TAG_PATH = "path";
    static final String TAG_PREFIX = "prefix";
    static final String TAG_ENABLED = "enabled";
    static final String TAG_CONFIG = "config";
    static final String ATTR_VERSION = "version";
    static final String VAL_BUILTIN = "built in";
    static final String VAL_EXTERNAL_ZIP = "external zip";

    static Element getElement(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        Element element;
        if (list.getLength() == 0) {
            element = xml.createElement(tag);
            parent.appendChild(element);
        } else {
            element = (Element) list.item(0);
        }
        return element;
    }

    static String getText(Node node) {
        if (node != null) {
            switch (node.getNodeType()) {
                case Node.TEXT_NODE:
                    return ((Text) node).getData();

                case Node.ATTRIBUTE_NODE:
                    return ((Attr) node).getValue();

                case Node.ELEMENT_NODE:
                    NodeList list = node.getChildNodes();
                    for (int i = 0; i < list.getLength(); i++) {
                        Node node1 = list.item(i);
                        if (node1.getNodeType() == Node.TEXT_NODE) {
                            return ((Text) node).getData();
                        }
                    }

                default:
                    break;
            }
        }
        return null;
    }

    static String getText(Element parent, String tag) {
        return getText(getElement(parent, tag));
    }

    static Element getRoot() {
        return getElement(xml.getDocumentElement(), TAG_ROOT);
    }

    static Element getConfig() {
        return getElement(getRoot(), TAG_CONFIG1);
    }

    static Element getConfig(String tag) {
        return getElement(getConfig(), tag);
    }

    static String getConfigValue(String tag) {
        return getText(getConfig(tag));
    }

    static Element getMods() {
        return getElement(getRoot(), TAG_MODS);
    }

    static Element getMod(String mod) {
        Element parent = getMods();
        NodeList list = parent.getElementsByTagName(TAG_MOD);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (mod.equals(getText(element, TAG_NAME))) {
                    return element;
                }
            }
        }
        Element element = xml.createElement(TAG_MOD);
        parent.appendChild(element);
        Element element1 = xml.createElement(TAG_NAME);
        element.appendChild(element1);
        Text text = xml.createTextNode(mod);
        element1.appendChild(text);
        return element;
    }

    static Element getModConfig(String mod) {
        return getElement(getMod(mod), TAG_CONFIG1);
    }

    static String getModConfigValue(String mod, String tag) {
        return getText(getModConfig(mod), tag);
    }

    private static void buildNewProperties() {
        Element root = xml.createElement(TAG_ROOT);
        xml.appendChild(root);
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
        String value = defaultValue.toString();
        if (!properties.containsKey(key)) {
            properties.setProperty(key, value);
            needSaveProps = true;
        }
        return properties.getProperty(key, value);
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
            value = Integer.parseInt(getString(mod, name, defaultValue));
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
        String value = getString(mod, name, defaultValue).toLowerCase();
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
        String key = getPropertyKey(mod, name);
        String oldValue = properties.getProperty(key);
        String newValue = value.toString();
        properties.setProperty(key, newValue);
        if (!newValue.equals(oldValue)) {
            needSaveProps = true;
        }
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
