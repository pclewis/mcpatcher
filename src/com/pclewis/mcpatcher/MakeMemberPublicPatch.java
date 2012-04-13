package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassPatch that changes the access flags of a particular member field or method.  Default
 * behavior is to make the member public.
 */
public class MakeMemberPublicPatch extends ClassPatch {
    private static final HashMap<String, Integer> accessFlags = new HashMap<String, Integer>() {
        {
            for (Field f : AccessFlag.class.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (f.getGenericType() == Integer.TYPE && Modifier.isPublic(mod) && Modifier.isStatic(mod) && Modifier.isFinal(mod)) {
                    try {
                        put(f.getName().toLowerCase(), f.getInt(null));
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
    };

    private JavaRef member;
    private String type;
    private int oldFlags;
    private int newFlags;

    /**
     * @param fieldRef may use deobfuscated names, provided they are in the class map
     */
    public MakeMemberPublicPatch(FieldRef fieldRef) {
        member = fieldRef;
        type = "field";
        optional = true;
    }

    /**
     * @param methodRef may use deobfuscated names, provided they are in the class map
     */
    public MakeMemberPublicPatch(MethodRef methodRef) {
        member = methodRef;
        type = "method";
        optional = true;
    }

    @Override
    public String getDescription() {
        int changes = oldFlags ^ newFlags;
        StringBuilder s = new StringBuilder();
        s.append("make ").append(type).append(" ").append(member.getName()).append(" ");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : accessFlags.entrySet()) {
            String name = entry.getKey();
            int flag = entry.getValue();
            if ((changes & flag) != 0) {
                if ((oldFlags & flag) != 0 && (flag == AccessFlag.PRIVATE || flag == AccessFlag.PROTECTED)) {
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    s.append(", ");
                }
                if ((oldFlags & flag) != 0) {
                    s.append("not ");
                }
                s.append(name);
            }
        }
        return s.toString();
    }

    @Override
    public boolean apply(ClassFile classFile) throws BadBytecode {
        classMod.classFile = classFile;
        classMod.methodInfo = null;
        JavaRef target = map(member);
        boolean patched = false;
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
