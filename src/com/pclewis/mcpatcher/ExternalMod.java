package com.pclewis.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ExternalMod extends Mod {
    private ZipFile zipFile;
    private String prefix;

    public ExternalMod(ZipFile zipFile, String prefix) {
        this.zipFile = zipFile;
        this.prefix = prefix;

        name = new File(zipFile.getName()).getName().replaceFirst("\\.[^.]+$", "");
        if (!prefix.equals("")) {
            description = String.format("Copy files from %s folder", prefix);
        }

        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix)) {
                String suffix = name.substring(prefix.length());
                filesToAdd.add(suffix);
            }
        }
    }

    @Override
    public InputStream openFile(String filename) throws IOException {
        String path = prefix + filename.replaceFirst("^/", "");
        return zipFile.getInputStream(new ZipEntry(path));
    }
}
