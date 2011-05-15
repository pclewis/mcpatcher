package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.mod.BaseMod;
import com.pclewis.mcpatcher.mod.BetterGrass;
import com.pclewis.mcpatcher.mod.HDFont;
import com.pclewis.mcpatcher.mod.HDTexture;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.lang.model.element.ElementVisitor;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

class ModList {
    private Vector<Mod> modsByIndex = new Vector<Mod>();
    private HashMap<String, Mod> modsByName = new HashMap<String, Mod>();
    private boolean applied = false;

    Mod baseMod;

    public ModList() {
        baseMod = new BaseMod();
        baseMod.internal = true;
        baseMod.setEnabled(true);
        addNoReplace(baseMod);
    }

    public void loadBuiltInMods() {
        if (!modsByName.containsKey(MCPatcherUtils.VAL_HD_TEXTURES)) {
            addNoReplace(new HDTexture());
        }
        if (!modsByName.containsKey(MCPatcherUtils.VAL_HD_FONT)) {
            addNoReplace(new HDFont());
        }
        if (!modsByName.containsKey(MCPatcherUtils.VAL_BETTER_GRASS)) {
            addNoReplace(new BetterGrass());
        }
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
                if (mod != null && addNoReplace(mod)) {
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
        Vector<Mod> visibleMods = new Vector<Mod>();
        for (Mod mod : modsByIndex) {
            if (!mod.internal) {
                visibleMods.add(mod);
            }
        }
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
            if (enabled) {
                if (enableAll) {
                    selectMod(mod, true);
                }
            } else {
                selectMod(mod, false);
            }
        }
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public void remove(Mod mod) {
        String name = mod.getName();
        for (int i = 0; i < modsByIndex.size(); i++) {
            if (modsByIndex.get(i) == mod) {
                modsByIndex.remove(i);
                modsByName.remove(name);
            }
        }
    }

    public int addFirst(Mod mod) {
        String name = mod.getName();
        Mod oldMod = modsByName.get(name);
        if (oldMod != null) {
            remove(oldMod);
        }
        for (int i = 0; i < modsByIndex.size(); i++) {
            oldMod = modsByIndex.get(i);
            if (!oldMod.internal && !(oldMod instanceof ExternalMod)) {
                modsByIndex.add(i, mod);
                break;
            }
        }
        modsByName.put(name, mod);
        mod.setRefs();
        return indexOfVisible(mod);
    }

    public int addLast(Mod mod) {
        String name = mod.getName();
        Mod oldMod = modsByName.get(name);
        if (oldMod != null) {
            remove(oldMod);
        }
        modsByIndex.add(mod);
        modsByName.put(name, mod);
        mod.setRefs();
        return indexOfVisible(mod);
    }

    public int moveUp(int index) {
        return move(index, -1);
    }

    public int moveDown(int index) {
        return move(index, 1);
    }

    private int move(int index, int offset) {
        int newIndex = index + offset;
        Vector<Mod> visibleMods = getVisible();
        if (index >= 0 && index < visibleMods.size() && newIndex >= 0 && newIndex < visibleMods.size()) {
            Mod mod1 = visibleMods.get(index);
            Mod mod2 = visibleMods.get(newIndex);
            int i = -1;
            int j = -1;
            for (int k = 0; k < modsByIndex.size(); k++) {
                if (mod1 == modsByIndex.get(k)) {
                    i = k;
                }
                if (mod2 == modsByIndex.get(k)) {
                    j = k;
                }
            }
            if (i > 0 && j > 0) {
                modsByIndex.set(i, mod2);
                modsByIndex.set(j, mod1);
                index = newIndex;
            }
        }
        return index;
    }

    private boolean addNoReplace(Mod mod) {
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
        if (MCPatcherUtils.hasMod(mod.getName())) {
            String enabled = MCPatcherUtils.getText(MCPatcherUtils.getMod(mod.getName()), MCPatcherUtils.TAG_ENABLED);
            if (enabled == null) {
                mod.setEnabled(mod.defaultEnabled);
            } else {
                mod.setEnabled(Boolean.parseBoolean(enabled));
            }
        } else {
            MCPatcherUtils.getMods().appendChild(defaultModElement(mod));
        }
        mod.loadOptions();
        return true;
    }

    public int indexOf(Mod mod) {
        for (int i = 0; i < modsByIndex.size(); i++) {
            if (mod == modsByIndex.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfVisible(Mod mod) {
        Vector<Mod> visible = getVisible();
        for (int i = 0; i < visible.size(); i++) {
            if (mod == visible.get(i)) {
                return i;
            }
        }
        return -1;
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
        ArrayList<Element> invalidEntries = new ArrayList<Element>();
        for (int i = 0; i < list.getLength(); i++) {
            Element element = (Element) list.item(i);
            String name = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_NAME);
            String type = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_TYPE);
            Mod mod = null;
            if (name == null || type == null) {
                invalidEntries.add(element);
            } else if (type.equals(MCPatcherUtils.VAL_BUILTIN)) {
                if (name.equals(MCPatcherUtils.VAL_HD_TEXTURES)) {
                    mod = new HDTexture();
                } else if (name.equals(MCPatcherUtils.VAL_HD_FONT)) {
                    mod = new HDFont();
                } else if (name.equals(MCPatcherUtils.VAL_BETTER_GRASS)) {
                    mod = new BetterGrass();
                } else {
                    invalidEntries.add(element);
                }
            } else if (type.equals(MCPatcherUtils.VAL_EXTERNAL_ZIP)) {
                String path = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_PATH);
                String prefix = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_PREFIX);
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        try {
                            mod = new ExternalMod(new ZipFile(file), prefix);
                        } catch (IOException e) {
                            Logger.log(e);
                        }
                    }
                } else {
                    invalidEntries.add(element);
                }
            } else if (type.equals(MCPatcherUtils.VAL_EXTERNAL_JAR)) {
                String path = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_PATH);
                String className = MCPatcherUtils.getText(element, MCPatcherUtils.TAG_CLASS);
                if (path != null && className != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        mod = loadCustomMod(file, className);
                    }
                } else {
                    invalidEntries.add(element);
                }
            } else {
                invalidEntries.add(element);
            }
            if (mod != null) {
                addNoReplace(mod);
            }
        }
        for (Element element : invalidEntries) {
            mods.removeChild(element);
        }
    }

    private void updateModElement(Mod mod, Element element) {
        if (mod instanceof ExternalMod) {
            ExternalMod extmod = (ExternalMod) mod;
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_TYPE, MCPatcherUtils.VAL_EXTERNAL_ZIP);
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_PATH, extmod.zipFile.getName());
            MCPatcherUtils.setText(element, MCPatcherUtils.TAG_PREFIX, extmod.prefix);
        } else if (mod.customJar == null) {
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
