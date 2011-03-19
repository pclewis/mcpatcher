package com.pclewis.mcpatcher;

public class FilenameSignature extends ClassSignature {
    private String filename;

    public FilenameSignature(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
