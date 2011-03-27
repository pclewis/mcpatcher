package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.mod.HDFontMod;
import com.pclewis.mcpatcher.mod.HDTextureMod;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ModList {
    private static final String IGNORE_CLASS = "newcode";
    private static final int MAX_DEPENDENCY_DEPTH = 20;

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

    public void selectMod(Mod mod, boolean enabled) {
        HashMap<Mod, Boolean> old = new HashMap<Mod, Boolean>();
        for (Mod m : modsByIndex) {
            old.put(m, m.isEnabled());
        }
        try {
            if (enabled) {
                enableMod(mod, 0);
            } else {
                disableMod(mod, 0);
            }
        } catch (RuntimeException e) {
            Logger.log(e);
            for (Mod m : modsByIndex) {
                m.setEnabled(old.get(m));
            }
        }
    }

    private boolean enableMod(Mod mod, int depth) {
        if (depth > MAX_DEPENDENCY_DEPTH) {
            throw new RuntimeException("mod dependency depth exceeded");
        }
        if (mod == null) {
            return false;
        }
        Logger.log(Logger.LOG_MOD, "enabling %s (%s)", mod.getName(), (depth > 0 ? "auto" : "manual"));
        if (!mod.okToApply()) {
            return false;
        }
        for (Mod.Dependency dep : mod.dependencies) {
            Mod dmod = modsByName.get(dep.name);
            if (dep.enabled) {
                if (!enableMod(dmod, depth + 1)) {
                    return false;
                }
            } else {
                disableMod(dmod, depth + 1);
            }
        }
        mod.setEnabled(true);
        return true;
    }

    private void disableMod(Mod mod, int depth) {
        if (depth > MAX_DEPENDENCY_DEPTH) {
            throw new RuntimeException("mod dependency depth exceeded");
        }
        if (mod == null) {
            return;
        }
        Logger.log(Logger.LOG_MOD, "disabling %s (%s)", mod.getName(), (depth > 0 ? "auto" : "manual"));
        mod.setEnabled(false);
        for (Mod dmod : modsByIndex) {
            if (dmod != mod) {
                for (Mod.Dependency dep : dmod.dependencies) {
                    if (dep.name.equals(mod.getName()) && dep.enabled) {
                        disableMod(dmod, depth + 1);
                        break;
                    }
                }
            }
        }
    }
}
