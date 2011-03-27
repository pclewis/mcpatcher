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
        for (Mod mod : modsByIndex) {
            if (mod.okToApply()) {
                mod.setEnabled(true);
            }
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
}
