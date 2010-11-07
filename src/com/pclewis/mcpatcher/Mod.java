package com.pclewis.mcpatcher;

import javassist.CtClass;
import javassist.CtMethod;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * Base class for all mods. A mod can do the following things:
 *  - Identify classes and methods
 *  - Patch existing classes
 *  - Replace files
 *  - Add new files
 *
 * Any new code can additionally be run through a filter to map identified class and method names to the
 * obfuscated versions they correspond to.
 */
public abstract class Mod {
    /**
     * Files to patch using the default patchFile handler. The key may be either an obfuscated class name,
     * identified class name, or file name.
     */
    protected static Map<String, Patch> patches = null;

    /**
     * Identify a class and map it to a common name. The name may include packages by using a '.' character.
     * The default implementation does nothing.
     *
     * @param de        Deobfuscator instance.
     * @param ctClass   The class to be identified.
     * @return          A name to map to, or null.
     */
    public String identifyClass(Deobfuscator de, CtClass ctClass) {
        return null;
    }

    /**
     * Identify a method and map it to a common name.
     * @param de        Deobfuscator instance.
     * @param method    The method to be identified.
     * @return          A name to map to, or null.
     */
    public String identifyMethod(Deobfuscator de, CtMethod method) {
        return null;
    }

    /**
     * Allow user to configure mod in whatever manner is appropriate. Must set configurable=true in ModInfo annotation.
     * The default implementation does nothing.
     */
    public void configure() {
    }

    /**
     * Add any new files to the output jar.
     * The default implementation does nothing.
     *
     * @param target The output jar file
     */
    public void addFiles(JarFile target) {
    }

    /**
     * Replace a file.
     * The default implementation does nothing.
     *
     * @param name   The original file name.
     * @param input  Input stream for the original file.
     * @param output Output stream for new file.
     * @return       true if file was replaced, otherwise false.
     */
    public boolean replaceFile(String name, InputStream input, OutputStream output) {
        return false;
    }

    /**
     * Patch a class.
     * The default implementation will look for patches in the patches member variable and apply them.
     *
     * @param de      Deobfuscator instance.
     * @param ctClass The class to patch.
     */
    public void patchFile(Deobfuscator de, CtClass ctClass) {
        if(patches == null || patches.isEmpty())
            return;

        Patch patch = patches.get(ctClass.getName());
        if(patch == null) patch = patches.get(ctClass.getName() + ".class");
        if(patch == null) {
            for (String name : de.getClassFriendlyNames(ctClass.getName())) {
                patch = patches.get(name);
                if(patch != null) break;
            }
        }

        if(patch == null)
            return;

        try {
            patch.apply(ctClass);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}