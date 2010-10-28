package com.pclewis.mcpatcher.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class PathArchive implements Archive {
    File dir = null;

    public PathArchive(File dir) {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory");
        }
        this.dir = dir;
    }

    private static List<String> getPaths(File dir, String root) {
        List<String> paths = new LinkedList<String>();
        for(String name : dir.list()) {
            File f = new File(dir, name);
            if(f.isDirectory()) {
                String r = (root.length() == 0) ? ("") : (root + "/");
                paths.addAll( getPaths(f, r + f.getName() ));
            } else {
                paths.add( root + "/" + f.getName() );
            }
        }
        return paths;
    }

    public List<String> getPaths() {
        return getPaths(dir, "");
    }

    public InputStream getInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(dir, name));
    }

    public String getPath() {
        return dir.getAbsolutePath();
    }
}
