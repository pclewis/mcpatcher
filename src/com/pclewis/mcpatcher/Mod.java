package com.pclewis.mcpatcher;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

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
     * Optional GUI config screen
     */
    protected ModConfigPanel configPanel = null;
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

    protected boolean defaultEnabled = true;
    protected ClassMap classMap = new ClassMap();
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
     * See HDFont.java (simple) and HDTexture.java (much more complex) for full examples.
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
            classMod.mod = this;
            for (ClassSignature classSignature : classMod.classSignatures) {
                classSignature.setClassMod(classMod);
            }
            for (ClassPatch classPatch : classMod.patches) {
                classPatch.setClassMod(classMod);
            }
        }
    }

    void resetCounts() {
        for (ClassMod classMod : getClassMods()) {
            for (ClassPatch classPatch : classMod.patches) {
                classPatch.numMatches.clear();
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

    void loadOptions() {
        if (configPanel != null) {
            configPanel.load();
        }
    }

    /**
     * Called by MCPatcher for each file in filesToAdd and filesToReplace.  Default implementation
     * gets the class resource.
     *
     * @param name name of file to open
     * @return a valid input stream, or null
     * @throws IOException I/O error
     * @see #filesToAdd
     * @see #filesToReplace
     */
    public InputStream openFile(String name) throws IOException {
        Logger.log(Logger.LOG_MAIN, "DEBUG: attempting to open %s", name);
        URL url = getClass().getResource(name);
        if (url == null) {
            Logger.log(Logger.LOG_MAIN, "DEBUG: url is null");
            return null;
        }
        Logger.log(Logger.LOG_MAIN, "DEBUG: got url %s", url.toString());
        InputStream inputStream = url.openStream();
        if (inputStream == null) {
            Logger.log(Logger.LOG_MAIN, "DEBUG: openStream failed, retrying with getContextClassLoader");
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            if (inputStream == null) {
                Logger.log(Logger.LOG_MAIN, "DEBUG: failed again, giving up");
                return null;
            }
        }
        Logger.log(Logger.LOG_MAIN, "DEBUG: successfully opened %s", name);
        return inputStream;
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

    class Dependency {
        String name;
        boolean enabled;

        Dependency(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }
    }
}
