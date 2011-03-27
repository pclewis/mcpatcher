package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.mod.HDFontMod;
import com.pclewis.mcpatcher.mod.HDTextureMod;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ModList {
    private static final String IGNORE_CLASS = "newcode";

    private Vector<Mod> modsByIndex = new Vector<Mod>();
    private HashMap<String, Mod> modsByName = new HashMap<String, Mod>();
    private boolean applied = false;

    public void loadBuiltInMods() {
        add(new HDTextureMod());
        add(new HDFontMod());
    }

    public void loadCustomMods(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            for (File f : directory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".jar");
                }
            })) {
                try {
                    loadJar(f);
                } catch (Throwable e) {
                    Logger.log(Logger.LOG_JAR, "Error loading mods from %s", f.getPath());
                    Logger.log(e);
                }
            }
        }
    }

    private void loadJar(File file) throws Exception {
        Logger.log(Logger.LOG_JAR, "Opening %s", file.getPath());
        final JarFile jar = new JarFile(file);
        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, getClass().getClassLoader()) {
            @Override
            public Class<?> findClass(String name) {
                if (!name.startsWith(IGNORE_CLASS)) {
                    try {
                        return super.findClass(name);
                    } catch (ClassNotFoundException e) {
                        Logger.log(e);
                    }
                }
                return null;
            }
        };
        for (JarEntry entry : Collections.list(jar.entries())) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().startsWith(IGNORE_CLASS)) {
                Class<?> cl = loader.loadClass(ClassMap.filenameToClassName(entry.getName()));
                if (cl != null && !cl.isInterface() && Mod.class.isAssignableFrom(cl)) {
                    int flags = cl.getModifiers();
                    if (!Modifier.isAbstract(flags) && Modifier.isPublic(flags)) {
                        Logger.log(Logger.LOG_MOD, "new %s() from %s", cl.getName(), file.getPath());
                        Mod mod = (Mod) cl.newInstance();
                        add(mod);
                    }
                }
            }
        }
    }

    public Vector<Mod> getAll() {
        return modsByIndex;
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

    public void enableValidMods() {
        for (int i = modsByIndex.size() - 1; i >= 0; i--) {
            Mod mod = modsByIndex.get(i);
            selectMod(mod, mod.okToApply());
        }
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    private void add(Mod mod) {
        if (mod == null) {
            return;
        }
        String name = mod.getName();
        if (modsByName.containsKey(name)) {
            Logger.log(Logger.LOG_MOD, "WARNING: duplicate mod %s ignored", name);
            return;
        }
        mod.setRefs();
        modsByName.put(name, mod);
        modsByIndex.add(mod);
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
            entry.getKey().setEnabled(entry.getValue());
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
}
