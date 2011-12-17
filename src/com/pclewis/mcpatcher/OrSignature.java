package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;

public class OrSignature extends ClassSignature {
    private ClassSignature[] signatures;
    private ClassSignature matchedSignature;

    public OrSignature(ClassSignature... signatures) {
        this.signatures = signatures;
    }

    @Override
    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        for (ClassSignature signature : signatures) {
            if (signature.match(filename, classFile, tempClassMap)) {
                matchedSignature = signature;
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterMatch(ClassFile classFile) {
        if (matchedSignature != null) {
            matchedSignature.afterMatch(classFile);
        }
    }
}
