package com.pclewis.mcpatcher;

import com.pclewis.mcpatcher.archive.Archive;
import com.pclewis.mcpatcher.archive.PathArchive;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Maintain a library of versioned minecraft.jar files.
 */
public class MinecraftLibrary {
    private static Logger logger = Logger.getLogger("com.pclewis.mcpatcher.MinecraftLibrary");
    private File path;

    public MinecraftLibrary(File path) {
        if(!path.exists()) {
            if(!path.mkdirs()) {
                throw new RuntimeException("Can't create Library path: " + path.getName());
            }
        } else if (!path.isDirectory()) {
            throw new RuntimeException("Library path is not directory: " + path.getName());
        }

        this.path = path;
    }

    /**
     * Import file into library.
     * @param file File to import
     * @throws java.io.IOException if can't read file
     */
    public void importFile(File file) throws IOException {
        Minecraft mc = new Minecraft(file);
        Version v = mc.getVersion();

        if(v==null) {
            throw new IllegalArgumentException("Could not detect version of " + file.getName());
        }

        File outFile = new File(path, mc.createFilename());
        logger.fine("Import Minecraft jar " + v.toString() + ": " + file.getAbsolutePath() +
                    " -> " + outFile.getAbsolutePath());

        InputStream is = new FileInputStream(file);
        OutputStream os = new FileOutputStream(outFile);

        Util.copyStream(is, os);

        is.close();
        os.close();
    }


    /**
     * @return all Minecraft versions in library
     */
    public Minecraft[] getEntries() {
        List<Minecraft> entries = new LinkedList<Minecraft>();
        Archive a = new PathArchive(path);
        for(String p : a.getPaths()) {
            logger.fine(p);
            if(p.startsWith("/minecraft") && p.endsWith(".jar")) {
                entries.add( new Minecraft(new File(path, p)) );
            }
        }
        return entries.toArray(new Minecraft[entries.size()]);
    }

    /**
     * @param v version to check
     * @return true if passed version is higher than the highest in the library.
     */
    public boolean isNewer(Version v) {
        for(Minecraft omc : getEntries()) {
            if(omc.guessVersion().greaterThan(v))
                return false;
        }
        return true;
    }

    public File getPath() {
        return path;
    }
}
