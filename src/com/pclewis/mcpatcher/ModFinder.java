package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.archive.Archive;
import javassist.bytecode.ClassFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModFinder {
    private static Logger logger = Logger.getLogger("com.pclewis.mcpatcher.ModFinder");
    public static List<Mod> findMods(Archive source) {
        List<Mod> mods = new LinkedList<Mod>();

        for(String name : source.getPaths()) {
            if(!name.endsWith(".class"))
                continue;

            logger.finest("Looking for Mod in " + name);

            ClassFile cf = null;
            try {
                cf = new ClassFile(new DataInputStream(source.getInputStream(name)));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't load " + name + " in " + source + ", skipping", e);
                continue;
            }

            if(cf.getSuperclass().equals("com.pclewis.mcpatcher.Mod")) {
                ClassLoader cl;
                try {
                    cl = new URLClassLoader( new URL[] { new File(name).toURI().toURL() } );
                } catch (MalformedURLException e) {
                    logger.log(Level.SEVERE, "", e);
                    throw new RuntimeException(e.toString());
                }

                try {
                    Class c = cl.loadClass(cf.getName());
                    mods.add((Mod)c.newInstance());
                    logger.info("Loaded mod: " + cf.getName());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Can't load class " + cf.getName(), e);
                }
            }
        }

        return mods;
    }
}
