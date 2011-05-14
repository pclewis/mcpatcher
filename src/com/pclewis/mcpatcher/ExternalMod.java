package com.pclewis.mcpatcher;

import java.io.File;
import java.util.zip.ZipFile;

class ExternalMod extends Mod {
    private ZipFile zipFile;
    private String prefix;

    public ExternalMod(ZipFile zipFile, String prefix) {
        this.zipFile = zipFile;
        this.prefix = prefix;

        name = new File(zipFile.getName()).getName();
        description = String.format("Copy files from %s", prefix);
    }
}
