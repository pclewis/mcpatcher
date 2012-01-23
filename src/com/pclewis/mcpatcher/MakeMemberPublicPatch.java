package com.pclewis.mcpatcher;

import javassist.bytecode.*;

/**
 * ClassPatch that makes a particular member field or method public.
 */
public class MakeMemberPublicPatch extends ClassPatch {
    private JavaRef member;
    private String type;

    /**
     * @param fieldRef may use deobfuscated names, provided they are in the class map
     */
    public MakeMemberPublicPatch(FieldRef fieldRef) {
        member = fieldRef;
        type = "field";
    }

    /**
     * @param methodRef may use deobfuscated names, provided they are in the class map
     */
    public MakeMemberPublicPatch(MethodRef methodRef) {
        member = methodRef;
        type = "method";
    }

    @Override
    public String getDescription() {
        return String.format("make %s %s public", type, member.getName());
    }

    @Override
    public boolean apply(ClassFile classFile) throws BadBytecode {
        classMod.classFile = classFile;
        classMod.methodInfo = null;
        JavaRef target = map(member);
        boolean patched = false;
        int oldFlags;
        int newFlags;
        if (target instanceof FieldRef) {
            for (Object o : classFile.getFields()) {
                FieldInfo fi = (FieldInfo) o;
                if (fi.getName().equals(target.getName()) && fi.getDescriptor().equals(target.getType())) {
                    oldFlags = fi.getAccessFlags();
                    newFlags = getNewFlags(oldFlags);
                    if (oldFlags != newFlags) {
                        fi.setAccessFlags(newFlags);
                        patched = true;
                    }
                }
            }
        } else if (target instanceof MethodRef) {
            for (Object o : classFile.getMethods()) {
                MethodInfo mi = (MethodInfo) o;
                if (mi.getName().equals(target.getName()) && mi.getDescriptor().equals(target.getType())) {
                    oldFlags = mi.getAccessFlags();
                    newFlags = getNewFlags(oldFlags);
                    if (oldFlags != newFlags) {
                        mi.setAccessFlags(newFlags);
                        patched = true;
                    }
                }
            }
        }
        if (patched) {
            recordPatch();
        }
        return patched;
    }

    /**
     * Returns new set of access flags.  By default, this removes 'protected' and 'private' and adds 'public'.
     * Override this method to do something else.
     *
     * @param oldFlags old access flags
     * @return new access flags
     * @see javassist.bytecode.AccessFlag
     */
    public int getNewFlags(int oldFlags) {
        return (oldFlags & ~(AccessFlag.PRIVATE | AccessFlag.PROTECTED)) | AccessFlag.PUBLIC;
    }
}
