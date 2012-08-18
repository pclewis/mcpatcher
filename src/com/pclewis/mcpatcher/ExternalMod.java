package com.pclewis.mcpatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ExternalMod extends Mod {
    ZipFile zipFile;
    HashMap<String, String> fileMap;

    private String defaultDescription;

    public ExternalMod(ZipFile zipFile, HashMap<String, String> fileMap) {
        dependencies.clear();
        this.zipFile = zipFile;
        setFileMap(fileMap);
        name = new File(zipFile.getName()).getName().replaceFirst("\\.[^.]+$", "");
        Properties properties = new Properties();
        InputStream is = null;
        try {
            ZipEntry entry = zipFile.getEntry("mod.properties");
            if (entry != null) {
                is = zipFile.getInputStream(entry);
                properties.load(is);
                String value = properties.getProperty("name", "").trim();
                if (!value.equals("")) {
                    name = value;
                }
                author = properties.getProperty("author", "").trim();
                website = properties.getProperty("website", "").trim();
                version = properties.getProperty("version", "").trim();
                description = properties.getProperty("description", "").trim();
                for (String m : properties.getProperty("dependencies", "").split(",")) {
                    m = m.trim();
                    if (!m.equals("")) {
                        addDependency(m);
                    }
                }
                for (String m : properties.getProperty("conflicts", "").split(",")) {
                    m = m.trim();
                    if (!m.equals("")) {
                        addConflict(m);
                    }
                }
                String mcVersion = properties.getProperty("minecraft.version", "").trim();
                if (!mcVersion.equals("") && !mcVersion.equals(MCPatcher.minecraft.getVersion().getVersionString())) {
                    addError("Requires Minecraft " + mcVersion);
                }
            }
        } catch (IOException e) {
        } finally {
            MCPatcherUtils.close(is);
        }
    }

    void setFileMap(HashMap<String, String> newFileMap) {
        fileMap = new HashMap<String, String>();
        fileMap.putAll(newFileMap);
        filesToAdd.clear();
        filesToAdd.addAll(fileMap.keySet());
        int numFiles = this.fileMap.size();
        defaultDescription = String.format("%d file%s to add or replace.", numFiles, numFiles == 1 ? "" : "s");
    }

    @Override
    public String getDescription() {
        return description == null || description.equals("") ? defaultDescription : description;
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

    @Override
    public void close() {
        MCPatcherUtils.close(zipFile);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
