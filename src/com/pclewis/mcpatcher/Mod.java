package com.pclewis.mcpatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Base class for all mods.
 *
 * @see #Mod()
 */
public abstract class Mod {
    /**
     * Name of the mod as displayed in the MCPatcher UI
     */
    protected String name = "";
    /**
     * Author of the mod
     */
    protected String author = "";
    /**
     * URL of website or forum thread where mod is described
     */
    protected String website = "";
    /**
     * Mod version number as displayed in the MCPatcher UI
     */
    protected String version = "";
    /**
     * Brief description of the mod as displayed in the MCPatcher UI
     */
    protected String description = "";
    /**
     * List of ClassMod objects for the mod
     */
    protected ArrayList<ClassMod> classMods = new ArrayList<ClassMod>();
    /**
     * List of files to replace completely in the output minecraft.jar (rather than patching them)
     */
    protected ArrayList<String> filesToReplace = new ArrayList<String>();
    /**
     * List of files to add to the output minecraft.jar
     */
    protected ArrayList<String> filesToAdd = new ArrayList<String>();
    /**
     * List of directories in minecraft.jar to consider for patching (default: all directories in vanilla
     * minecraft.jar)<p/>
     * NOTE: Most mods will want to restrict themselves to files in the root of the jar:<br>
     * allowedDirs.clear();<br>
     * allowedDirs.add("");
     */
    protected ArrayList<String> allowedDirs = new ArrayList<String>() {
        {
            add("");
            add("com/jcraft/jogg");
            add("com/jcraft/jorbis");
            add("net/minecraft/client");
            add("net/minecraft/isom");
            add("paulscode/sound");
            add("paulscode/sound/codecs");
            add("paulscode/sound/libraries");
        }
    };

    protected ClassMap classMap = new ClassMap();
    private HashMap<String, String> params = new HashMap<String, String>();
    private ArrayList<String> errors = new ArrayList<String>();
    private boolean enabled = false;
    boolean internal = false;
    ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

    /**
     * Initialize mod.
     * <p/>
     * During initialization, the mod should<br>
     * - Assign values for basic mod information:
     * <pre>
     *     name = "Herobrine";
     *     author = "him@example.com";
     *     description = "Adds Herobrine to the game";
     *     version = "1.6";
     *     website = "http://www.example.com/";
     * </pre>
     * - Add any needed filenames to the filesToReplace or filesToAdd lists.  The mod's openFile
     * method should return a valid InputStream for the files listed here.
     * <pre>
     *     filesToReplace.add("gui/background.png");
     *     filesToAdd.add("HerobrineAI.class");
     * </pre>
     * - Create and add ClassMod objects to the classMods list.  Each ClassMod may in turn have
     * multiple ClassSignatures, ClassPatches.
     * <pre>
     *     class EntityMod extends ClassMod {
     *         ...
     *     }
     *     ...
     *     classMods.add(new EntityMod());
     * </pre>
     * - Specify which directories in minecraft.jar should be considered for patching.
     * By default, everything in vanilla minecraft.jar is considered for patching, including the
     * sound libs.  Most mods will want to restrict themselves to files in the root of the jar,
     * which reduces accidental bytecode matches and helps prevent mods from stepping on each
     * other:
     * <pre>
     *     allowedDirs.clear();
     *     allowedDirs.add("");
     * </pre>
     * <p/>
     * See HDFontMod.java (simple) and HDTextureMod.java (much more complex) for full examples.
     */
    public Mod() {
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getWebsite() {
        return website;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return mapping between readable class, method, and field names and obfuscated names
     */
    public ClassMap getClassMap() {
        return classMap;
    }

    void setRefs() {
        for (ClassMod classMod : getClassMods()) {
            classMod.setMod(this);
            for (ClassSignature classSignature : classMod.classSignatures) {
                classSignature.setClassMod(classMod);
            }
            for (ClassPatch classPatch : classMod.patches) {
                classPatch.setClassMod(classMod);
            }
        }
    }

    ArrayList<ClassMod> getClassMods() {
        return classMods;
    }

    boolean okToApply() {
        return errors.size() == 0 && getErrors().size() == 0;
    }

    void addError(String error) {
        errors.add(error);
    }

    ArrayList<String> getErrors() {
        ArrayList<String> errors = new ArrayList<String>(this.errors);
        for (ClassMod classMod : classMods) {
            if (!classMod.okToApply()) {
                for (String s : classMod.errors) {
                    errors.add(String.format("%s: %s", classMod.getDeobfClass(), s));
                }
            }
        }
        return errors;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Called by MCPatcher for each file in filesToAdd and filesToReplace.
     *
     * @param name name of file to open
     * @return a valid input stream, or null
     * @throws IOException I/O error
     * @see #filesToAdd
     * @see #filesToReplace
     */
    public InputStream openFile(String name) throws IOException {
        return null;
    }

    /**
     * Indicates that this mod <i>requires</i> another mod to function.  Whenever the specified
     * mod is unchecked in the GUI, this mod will be unchecked too.  Whenever this mod is checked
     * in the GUI, the specified mod will be checked also.  If the specified mod is not available
     * at all, then this mod will be unchecked and greyed out in the GUI.
     * <p/>
     * If Mod A calls <code>addDependency(B)</code>:<br>
     * check A -> check B<br>
     * uncheck B -> uncheck A<br>
     * B unavailable -> A greyed out<br>
     *
     * @param name name of required mod
     */
    final protected void addDependency(String name) {
        dependencies.add(new Dependency(name, true));
    }

    /**
     * Indicates that this mod <i>conflicts</i> with another mod.  Whenever the specified mod is
     * checked in the GUI, this mod will be unchecked.  Whenever this mod is checked in the GUI,
     * the specified mod will be unchecked.  If the specified mod is not available at all, then
     * there is no effect.
     * <p/>
     * If Mod A calls <code>addConflict(B)</code>:<br>
     * check A -> uncheck B<br>
     * check B -> uncheck A<br>
     * B unavailable -> no effect<br>
     *
     * @param name name of conflicting mod
     */
    final protected void addConflict(String name) {
        dependencies.add(new Dependency(name, false));
    }

    /**
     * Set a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name  parameter name
     * @param value parameter value (must support toString())
     */
    final protected void setModParam(String name, Object value) {
        params.put(name, value.toString());
    }

    /**
     * Get a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name parameter name
     * @return parameter value or ""
     */
    final protected String getModParam(String name) {
        String value = params.get(name);
        return value == null ? "" : value;
    }

    /**
     * Get a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name parameter name
     * @return parameter value or 0
     */
    final protected int getModParamInt(String name) {
        try {
            return Integer.parseInt(params.get(name));
        } catch (Throwable e) {
            return 0;
        }
    }

    /**
     * Get a global parameter for the mod, visible to all ClassMods and ClassPatches.
     *
     * @param name parameter name
     * @return parameter value or false
     */
    final protected boolean getModParamBool(String name) {
        try {
            return Boolean.parseBoolean(params.get(name));
        } catch (Throwable e) {
            return false;
        }
    }

    class Dependency {
        String name;
        boolean enabled;

        Dependency(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }
    }
}
