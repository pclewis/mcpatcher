package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.archive.Archive;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModFinder {
    private static Logger logger = Logger.getLogger("com.pclewis.mcpatcher.ModFinder");
    public static List<Class> findMods(Archive source) {
        List<Class> mods = new LinkedList<Class>();

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
                ClassPool cp = ClassPool.getDefault();
                try {
                    cp.appendClassPath(source.getPath());
                } catch (NotFoundException e) {
                    logger.log(Level.SEVERE, "Couldn't add " + source + " to classpath", e);
                    continue;
                }

                CtClass modClass = cp.makeClass(cf.getName());
                try {
                    mods.add( modClass.toClass() );
                } catch (CannotCompileException e) {
                    logger.log(Level.SEVERE, "Can't load class " + modClass.getName(), e);
                }

                logger.info("Found mod: " + modClass.getName());
            }
        }

        return mods;
    }
}
