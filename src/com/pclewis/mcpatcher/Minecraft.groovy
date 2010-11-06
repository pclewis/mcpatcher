package com.pclewis.mcpatcher;


import java.security.NoSuchAlgorithmException
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import javassist.bytecode.ClassFile
import javassist.bytecode.ConstPool

/**
 * A minecraft jar file. The underlying file is opened as needed so users
 * should be ready for IOExceptions if the file goes missing.
 */
public class Minecraft {
    private static Logger logger = Logger.getLogger("com.pclewis.mcpatcher.Minecraft");
    private Version version;
    private File file;

    public Minecraft(File file) {
        this.file = file;
    }

    /**
     * Guess version from file name (best generated from createFilename().
     * @return Guessed version, or null.
     */
    public Version guessVersion() {
        String v = file.getName().find(/minecraft\.([^.]+\.[0-9._]+)\./) { f,v -> v };
        return v ? new Version(v.replaceFirst(/\./," ")) : null;
    }

    /**
     * @return Version of Minecraft, or null if undetermined/invalid
     * @throws IOException if jar opening failed
     */
    public Version getVersion() throws IOException {
        if (version == null) {
            withJar() { JarFile jar ->
                ZipEntry zipEntry = jar.getEntry("net/minecraft/client/Minecraft.class");
                if (zipEntry == null) {
                    zipEntry = jar.getEntry("net/minecraft/server/MinecraftServer.class");
                    if(zipEntry == null) {
                        logger.severe("Can't find client/Minecraft.class or server/MinecraftServer.class");
                        return;
                    }
                }

                InputStream is;
                try {
                    is = jar.getInputStream(zipEntry);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Can't open ${zipEntry.getName()}", e);
                    return;
                }

                ClassFile cf = new ClassFile(new DataInputStream(is));
                ConstPool cp = cf.getConstPool();
                for(int i = 1; i < cp.getSize(); ++i) {
                    if(cp.getTag(i) == ConstPool.CONST_String) {
                        String s = cp.getStringInfo(i);
                        String v = s.find(/(?i)Minecraft Minecraft (Alpha v.*)/) ?:
                                   s.find(/(?i)Minecraft (server version .*)/);
                        if(v!=null) {
                            version = new Version(v);
                            return;
                        }
                    }
                }
            }
        }
        return version;
    }

    /**
     * Open jar file, pass to closure, and ensure it is closed afterward.
     * @param c closure to run
     * @throws IOException if jar opening failed
     */
    private Object withJar(Closure c) throws IOException {
        JarFile jar = new JarFile(file);
        Object result;

        try {
            result = c(jar);
        } finally {
            jar.close();
        }
        return result;
    }

    /**
     * @param file The file to test
     * @return true if jar verification passed
     * @throws IOException  if I/O error has occurred
     */
    public boolean isPure() throws IOException {
        return (Boolean) withJar() { JarFile jar ->
            Manifest man;
            try {
                InputStream is = jar.getInputStream(jar.getEntry("META-INF/MANIFEST.MF"));
                man = new Manifest(is);
                is.close();
            } catch (SecurityException ignore) {
                return false;
            }

            Set<String> signed = new HashSet<String>();
            for (Map.Entry<String, Attributes> entry: man.getEntries().entrySet()) {
                for (Object attrkey: entry.getValue().keySet()) {
                    if (attrkey instanceof Attributes.Name &&
                            attrkey.toString().indexOf("-Digest") != -1)
                        signed.add(entry.getKey());
                }
            }

            Set<String> entries = new HashSet<String>();
            for (Enumeration<JarEntry> entry = jar.entries(); entry.hasMoreElements();) {
                JarEntry je = entry.nextElement();
                if (!je.isDirectory() && !je.getName().startsWith("META-INF/"))
                    entries.add(je.getName());
            }

            return signed.containsAll(entries);
        }
    }

    public String createFilename() throws IOException {
        String filename = "minecraft";

        filename += "." + this.getVersion().toString().replaceAll(" ", ".");

        if(!isPure()) {
            filename += ".modded";
        }

        try {
            filename += "." + Util.fileDigest(file, "MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "no MD5", e);
            throw new RuntimeException(e.toString());
        }

        return filename + ".jar";
    }

    public File getFile() {
        return file;
    }
}
