package com.pclewis.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ExternalMod extends Mod {
    ZipFile zipFile;
    HashMap<String, String> fileMap;

    public ExternalMod(ZipFile zipFile, HashMap<String, String> fileMap) {
        this.zipFile = zipFile;
        this.fileMap = new HashMap<String, String>();
        this.fileMap.putAll(fileMap);

        name = new File(zipFile.getName()).getName().replaceFirst("\\.[^.]+$", "");
        description = String.format("%d files to add or replace.", this.fileMap.size());

        filesToAdd.addAll(this.fileMap.keySet());
    }

    @Override
    public InputStream openFile(String filename) throws IOException {
        String path = fileMap.get(filename.replaceFirst("^/", ""));
        if (path == null) {
            return null;
        } else {
            return zipFile.getInputStream(new ZipEntry(path));
        }
    }
}
