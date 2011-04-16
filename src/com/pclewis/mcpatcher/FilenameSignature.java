package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;

/**
 * ClassSignature that matches by filename.
 */
public class FilenameSignature extends ClassSignature {
    protected String filename;

    public FilenameSignature(String filename) {
        this.filename = filename;
    }

    @Override
    boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        return filename.equals(this.filename);
    }
}
