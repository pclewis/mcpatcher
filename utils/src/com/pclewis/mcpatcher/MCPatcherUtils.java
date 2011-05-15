package com.pclewis.mcpatcher;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * Collection of static methods available to mods at runtime.  This class is always injected into
 * the output minecraft jar.
 */
public class MCPatcherUtils {
    private static File minecraftDir = null;
    private static File xmlFile = null;
    private static Document xml;
    private static boolean debug = false;
    private static boolean isGame;

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
    public static final String TAG_DEBUG = "debug";
    public static final String TAG_JAVA_HEAP_SIZE = "java-heap-size";
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
    static final String VAL_BUILTIN = "built-in";
    static final String VAL_EXTERNAL_ZIP = "external-zip";
    static final String VAL_EXTERNAL_JAR = "external-jar";

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
        if (minecraftDir != null && minecraftDir.exists()) {
            xmlFile = new File(minecraftDir, "mcpatcher.xml");
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                if (xmlFile.exists()) {
                    xml = builder.parse(xmlFile);
                } else {
                    xml = builder.newDocument();
                    buildNewProperties();
                }
            } catch (Exception e) {
                xml = null;
                e.printStackTrace();
            }

            File propFile = new File(minecraftDir, "mcpatcher.properties");
            if (propFile.exists()) {
                FileInputStream is = null;
                try {
                    is = new FileInputStream(propFile);
                    convertPropertiesToXML(is);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //propFile.delete();
                }
            }

            debug = getBoolean("debug", false);
            saveProperties();
            return true;
        }
        return false;
    }

    private static void convertPropertiesToXML(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String tag = entry.getKey().toString();
            String value = entry.getValue().toString();
            if (tag.equals("debug")) {
                set(TAG_DEBUG, value);
            } else if (tag.equals("lastVersion")) {
                set(TAG_LAST_VERSION, value);
            } else if (tag.equals("betaWarningShown")) {
                set(TAG_BETA_WARNING_SHOWN, value);
            } else if (tag.equals("heapSize")) {
                set(TAG_JAVA_HEAP_SIZE, value);
            } else if (tag.startsWith("HDTexture.")) {
                tag = tag.substring(10);
                if (!tag.equals("enabled")) {
                    set("HD Textures", tag, value);
                }
            }
        }
    }

    static Element getElement(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
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
        if (node == null) {
            return null;
        }
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
                        return ((Text) node1).getData();
                    }
                }

            default:
                break;
        }
        return null;
    }

    static void setText(Element parent, String tag, String value) {
        if (parent == null) {
            return;
        }
        Element element = getElement(parent, tag);
        while (element.hasChildNodes()) {
            element.removeChild(element.getFirstChild());
        }
        Text text = xml.createTextNode(value);
        element.appendChild(text);
    }

    static void remove(Node node) {
        if (node != null) {
            Node parent = node.getParentNode();
            parent.removeChild(node);
        }
    }

    static String getText(Element parent, String tag) {
        return getText(getElement(parent, tag));
    }

    static Element getRoot() {
        Element root = xml.getDocumentElement();
        if (root == null) {
            root = xml.createElement(TAG_ROOT);
            xml.appendChild(root);
        }
        return root;
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

    static void setConfigValue(String tag, String value) {
        Element element = getConfig(tag);
        if (element != null) {
            while (element.hasChildNodes()) {
                element.removeChild(element.getFirstChild());
            }
            element.appendChild(xml.createTextNode(value));
        }
    }

    static Element getMods() {
        return getElement(getRoot(), TAG_MODS);
    }

    static boolean hasMod(String mod) {
        Element parent = getMods();
        if (parent != null) {
            NodeList list = parent.getElementsByTagName(TAG_MOD);
            for (int i = 0; i < list.getLength(); i++) {
                Element element = (Element) list.item(i);
                NodeList list1 = element.getElementsByTagName(TAG_NAME);
                if (list1.getLength() > 0) {
                    element = (Element) list1.item(0);
                    if (mod.equals(getText(element))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static Element getMod(String mod) {
        Element parent = getMods();
        if (parent == null) {
            return null;
        }
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

    static boolean isModEnabled(String mod) {
        return Boolean.parseBoolean(getText(getMod(mod), TAG_ENABLED));
    }

    static void setModEnabled(String mod, boolean enabled) {
        setText(getMod(mod), TAG_ENABLED, Boolean.toString(enabled));
    }

    static Element getModConfig(String mod) {
        return getElement(getMod(mod), TAG_CONFIG1);
    }

    static Element getModConfig(String mod, String tag) {
        return getElement(getModConfig(mod), tag);
    }

    static String getModConfigValue(String mod, String tag) {
        return getText(getModConfig(mod, tag));
    }

    static void setModConfigValue(String mod, String tag, String value) {
        Element element = getModConfig(mod, tag);
        if (element != null) {
            while (element.hasChildNodes()) {
                element.removeChild(element.getFirstChild());
            }
            element.appendChild(xml.createTextNode(value));
        }
    }

    private static void buildNewProperties() {
        if (xml != null) {
            getRoot();
            getConfig();
            getMods();
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
        if (mod == null) {
            return getString(tag, defaultValue);
        }
        String value = getModConfigValue(mod, tag);
        if (value == null) {
            value = defaultValue.toString();
            setModConfigValue(mod, tag, value);
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
        String value = getConfigValue(tag);
        if (value == null) {
            value = defaultValue.toString();
            setConfigValue(tag, value);
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
        return getInt(null, tag, defaultValue);
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
        return getBoolean(null, tag, defaultValue);
    }

    /**
     * Sets a value in mcpatcher.xml.
     *
     * @param mod   name of mod
     * @param tag   property name
     * @param value property value (must support toString())
     */
    public static void set(String mod, String tag, Object value) {
        if (mod == null) {
            set(tag, value);
            return;
        }
        setModConfigValue(mod, tag, value.toString());
    }

    /**
     * Set a global config value in mcpatcher.xml.
     *
     * @param tag   property name
     * @param value property value (must support toString())
     */
    static void set(String tag, Object value) {
        setConfigValue(tag, value.toString());
    }

    /**
     * Remove a value from mcpatcher.xml.
     *
     * @param mod name of mod
     * @param tag property name
     */
    public static void remove(String mod, String tag) {
        if (mod == null) {
            remove(mod);
        } else {
            remove(getModConfig(mod, tag));
        }
    }

    /**
     * Remove a global config value from mcpatcher.xml.
     *
     * @param tag property name
     */
    static void remove(String tag) {
        remove(getConfig(tag));
    }

    /**
     * Save all properties to mcpatcher.xml.
     *
     * @return true if successful
     */
    public static boolean saveProperties() {
        boolean saved = false;
        if (xml != null && xmlFile != null) {
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(xmlFile);
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer trans = factory.newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(xml);
                trans.transform(source, new StreamResult(os));
                saved = true;
                System.out.printf("wrote %s\n", xmlFile.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return saved;
    }
}
