package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.mod.BaseMod;
import com.pclewis.mcpatcher.mod.BetterGrass;
import com.pclewis.mcpatcher.mod.HDFont;
import com.pclewis.mcpatcher.mod.HDTexture;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ModList {
    private Vector<Mod> modsByIndex = new Vector<Mod>();
    private Vector<Mod> visibleMods = new Vector<Mod>();
    private HashMap<String, Mod> modsByName = new HashMap<String, Mod>();
    private boolean applied = false;

    Mod baseMod;

    public ModList() {
        baseMod = new BaseMod();
        baseMod.internal = true;
        add(baseMod);
    }

    public void loadBuiltInMods() {
        add(new HDTexture());
        add(new HDFont());
        add(new BetterGrass());
    }

    public void loadCustomMods(File directory) {
        if (directory.isDirectory()) {
            for (File f : directory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".jar");
                }
            })) {
                try {
                    loadCustomModsFromJar(f);
                } catch (Throwable e) {
                    Logger.log(Logger.LOG_JAR, "Error loading mods from %s", f.getPath());
                    Logger.log(e);
                }
            }
        }
    }

    private void loadCustomModsFromJar(File file) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Logger.log(Logger.LOG_JAR, "Opening %s", file.getPath());
        final JarFile jar = new JarFile(file);
        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
        for (JarEntry entry : Collections.list(jar.entries())) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                Mod mod = loadCustomMod(loader, ClassMap.filenameToClassName(entry.getName()));
                if (mod != null && add(mod)) {
                    Logger.log(Logger.LOG_MOD, "new %s()", mod.getClass().getName());
                }
            }
        }
    }

    private Mod loadCustomMod(File file, String className) {
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader());
            return loadCustomMod(loader, className);
        } catch (Exception e) {
            Logger.log(e);
        }
        return null;
    }

    private Mod loadCustomMod(URLClassLoader loader, String className) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> cl = null;
        try {
            cl = loader.loadClass(className);
        } catch (NoClassDefFoundError e) {
            Logger.log(Logger.LOG_MOD, "WARNING: skipping %s: %s", className, e.toString());
        }
        if (cl != null && !cl.isInterface() && Mod.class.isAssignableFrom(cl)) {
            int flags = cl.getModifiers();
            if (!Modifier.isAbstract(flags) && Modifier.isPublic(flags)) {
                return (Mod) cl.newInstance();
            }
        }
        return null;
    }

    public Vector<Mod> getAll() {
        return modsByIndex;
    }

    public Vector<Mod> getVisible() {
        return visibleMods;
    }

    public ArrayList<Mod> getSelected() {
        ArrayList<Mod> list = new ArrayList<Mod>();
        for (Mod mod : modsByIndex) {
            if (mod.okToApply() && mod.isEnabled()) {
                list.add(mod);
            }
        }
        return list;
    }

    public Mod get(String name) {
        return modsByName.get(name);
    }

    public Mod get(int index) {
        return modsByIndex.get(index);
    }

    public int size() {
        return modsByIndex.size();
    }

    public void enableValidMods(boolean enableAll) {
        for (int i = modsByIndex.size() - 1; i >= 0; i--) {
            Mod mod = modsByIndex.get(i);
            boolean enabled = mod.okToApply();
            if (enabled && !enableAll) {
                //String name = mod.getName();
                //enabled = MCPatcherUtils.isModEnabled(mod.getName());
            }
            selectMod(mod, enabled);
        }
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    private boolean add(Mod mod) {
        if (mod == null) {
            return false;
        }
        String name = mod.getName();
        if (modsByName.containsKey(name)) {
            Logger.log(Logger.LOG_MOD, "WARNING: duplicate mod %s ignored", name);
            return false;
        }
        mod.setRefs();
        modsByName.put(name, mod);
        modsByIndex.add(mod);
        if (!mod.internal) {
            visibleMods.add(mod);
        }
        if (!MCPatcherUtils.hasMod(mod.getName())) {
            MCPatcherUtils.getMods().appendChild(defaultModElement(mod));
        }
        mod.loadOptions();
        return true;
    }

    private class ModDependencyException extends Exception {
        ModDependencyException(String s) {
            super(s);
        }
    }

    public void selectMod(Mod mod, boolean enabled) {
        HashMap<Mod, Boolean> inst = new HashMap<Mod, Boolean>();
        try {
            if (enabled) {
                enableMod(inst, mod, false);
            } else {
                disableMod(inst, mod, false);
            }
        } catch (ModDependencyException e) {
            Logger.log(e);
        }
        for (Map.Entry<Mod, Boolean> entry : inst.entrySet()) {
            mod = entry.getKey();
            mod.setEnabled(entry.getValue());
        }
    }

    private void enableMod(HashMap<Mod, Boolean> inst, Mod mod, boolean recursive) throws ModDependencyException {
        if (mod == null) {
            return;
        }
        //Logger.log(Logger.LOG_MOD, "%senabling %s", (recursive ? " " : ""), mod.getName());
        if (!mod.okToApply()) {
            throw new ModDependencyException(mod.getName() + " cannot be applied");
        }
        if (inst.containsKey(mod)) {
            if (!inst.get(mod)) {
                throw new ModDependencyException(mod.getName() + " is both conflicting and required");
            }
            return;
        } else {
            inst.put(mod, true);
        }
        for (Mod.Dependency dep : mod.dependencies) {
            Mod dmod = modsByName.get(dep.name);
            if (dep.enabled) {
                if (dmod == null) {
                    throw new ModDependencyException("dependent mod " + dep.name + " not available");
                } else {
                    enableMod(inst, dmod, true);
                }
            } else {
                disableMod(inst, dmod, true);
            }
        }
    }

    private void disableMod(HashMap<Mod, Boolean> inst, Mod mod, boolean recursive) throws ModDependencyException {
        if (mod == null) {
            return;
        }
        //Logger.log(Logger.LOG_MOD, "%sdisabling %s", (recursive ? " " : ""), mod.getName());
        if (inst.containsKey(mod)) {
            if (inst.get(mod)) {
                throw new ModDependencyException(mod.getName() + " is both conflicting and required");
            }
            return;
        } else {
            inst.put(mod, false);
        }
        for (Mod dmod : modsByIndex) {
            if (dmod != mod) {
                for (Mod.Dependency dep : dmod.dependencies) {
                    if (dep.name.equals(mod.getName()) && dep.enabled) {
                        disableMod(inst, dmod, true);
                        break;
                    }
                }
            }
        }
    }

    public void loadSavedMods() {
        Element mods = MCPatcherUtils.getMods();
        if (mods == null) {
            return;
        }
        NodeList list = mods.getElementsByTagName(MCPatcherUtils.TAG_MOD);
        for (int i = 0; i < list.getLength(); i++) {
            Element element = (Element) list.item(i);
            String name = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_NAME);
            String type = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_TYPE);
            Mod mod = null;
            if (name == null || type == null) {
            } else if (type.equals(MCPatcherUtils.VAL_BUILTIN)) {
                if (name.equals("HD Textures")) {
                    mod = new HDTexture();
                } else if (name.equals("HD Font")) {
                    mod = new HDFont();
                } else if (name.equals("Better Grass")) {
                    mod = new BetterGrass();
                }
            } else if (type.equals(MCPatcherUtils.VAL_EXTERNAL_JAR)) {
                String path = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_PATH);
                String className = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_CLASS);
                if (path != null && className != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        mod = loadCustomMod(file, className);
                    }
                }
            }
            if (mod != null) {
                add(mod);
            }
        }
    }

    private void updateModElement(Mod mod, Element element) {
        if (mod.customJar == null) {
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_TYPE, MCPatcherUtils.VAL_BUILTIN);
        } else {
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_TYPE, MCPatcherUtils.VAL_EXTERNAL_JAR);
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_PATH, mod.customJar.getName());
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_CLASS, mod.getClass().getCanonicalName());
        }
    }

    private Element defaultModElement(Mod mod) {
        Element mods = MCPatcherUtils.getMods();
        if (mods == null) {
            return null;
        }
        Element element = MCPatcherUtils.getMod(mod.getName());
        MCPatcherUtils.setText(element, MCPatcherUtils.TAG_ENABLED, Boolean.toString(mod.defaultEnabled));
        updateModElement(mod, element);
        return element;
    }

    void updateProperties() {
        Element mods = MCPatcherUtils.getMods();
        if (mods == null) {
            return;
        }
        HashMap<String, Element> oldElements = new HashMap<String, Element>();
        while (mods.hasChildNodes()) {
            Node node = mods.getFirstChild();
            if (node instanceof Element) {
                Element element = (Element) node;
                String name = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_NAME);
                if (name != null) {
                    oldElements.put(name, element);
                }
            }
            mods.removeChild(node);
        }
        for (Mod mod : modsByIndex) {
            if (mod.internal) {
                continue;
            }
            Element element = oldElements.get(mod.getName());
            if (element == null) {
                defaultModElement(mod);
            } else {
                MCPatcherUtils.setText(element, MCPatcherUtils.TAG_ENABLED, Boolean.toString(mod.isEnabled() && mod.okToApply()));
                updateModElement(mod, element);
                mods.appendChild(element);
                oldElements.remove(mod.getName());
            }
        }
    }
}
