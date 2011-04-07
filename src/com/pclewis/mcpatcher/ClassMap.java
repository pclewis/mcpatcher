package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarOutputStream;

import static javassist.bytecode.Opcode.*;

/**
 * Contains mapping from descriptive class, method, and field names to their obfuscated
 * names in minecraft.jar.  Each Mod has its own ClassMap that is maintained by MCPatcher.
 */
public class ClassMap {
    /**
     * Alias for default package.  Allows mods to refer to Minecraft classes without being
     * in the default package themselves.  Either <tt>RenderEngine</tt> or
     * <tt>net.minecraft.src.RenderEngine</tt> will map to the same obfuscated name at
     * runtime.
     */
    public static final String DEFAULT_MINECRAFT_PACKAGE = "net.minecraft.src";

    private HashMap<String, ClassMapEntry> classMap = new HashMap<String, ClassMapEntry>();

    ClassMap() {
    }

    /**
     * Convert a path to a .class file into a fully qualified class name.
     * e.g., net/minecraft/src/Minecraft.class -> net.minecraft.src.Minecraft
     *
     * @param filename
     * @return class name
     */
    public static String filenameToClassName(String filename) {
        return filename.replaceAll("\\.class$", "").replaceAll("/", ".");
    }

    /**
     * Convert a fully qualified class name into a path to a .class file.
     * e.g., net.minecraft.src.Minecraft -> net/minecraft/src/Minecraft.class
     *
     * @param className dotted name of package/class
     * @return filename
     */
    public static String classNameToFilename(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    /**
     * Add a class mapping.
     * <p/>
     * NOTE: It is not normally necessary to call this method explicitly.  ClassSignatures
     * implicitly create a mapping when they are resolved.
     *
     * @param descName descriptive class name
     * @param obfName  obfuscated class name
     */
    public void addClassMap(String descName, String obfName) {
        ClassMapEntry e = classMap.get(descName);
        if (e == null) {
            e = new ClassMapEntry(obfName);
            classMap.put(descName, e);
            if (!descName.contains(".")) {
                classMap.put(DEFAULT_MINECRAFT_PACKAGE + "." + descName, e);
            }
        }
    }

    /**
     * Add a method mapping.  The class mapping must already exist.
     * <p/>
     * NOTE: It is not normally necessary to call this method explicitly.  ClassMod.MethodMappers
     * implicitly create a mapping when they are resolved.
     *
     * @param classDescName descriptive class name
     * @param descName      descriptive method name
     * @param obfName       obfuscated method name
     * @throws RuntimeException if class mapping does not exist yet.
     */
    public void addMethodMap(String classDescName, String descName, String obfName) {
        ClassMapEntry e = classMap.get(classDescName);
        if (e == null) {
            throw new RuntimeException(String.format(
                "cannot add method map %s.%s -> %s because there is no class map for %s",
                classDescName, descName, obfName, classDescName
            ));
        }
        e.methodMap.put(descName, obfName);
    }

    /**
     * Add a field mapping.  The class mapping must already exist.
     * <p/>
     * NOTE: It is not normally necessary to call this method explicitly.  ClassMod.FieldMappers
     * implicitly create a mapping when they are resolved.
     *
     * @param classDescName descriptive class name
     * @param descName      descriptive field name
     * @param obfName       obfuscated field name
     * @throws RuntimeException if class mapping does not exist yet.
     */
    public void addFieldMap(String classDescName, String descName, String obfName) {
        ClassMapEntry e = classMap.get(classDescName);
        if (e == null) {
            throw new RuntimeException(String.format(
                "cannot add field map %s.%s -> %s because there is no class map for %s",
                classDescName, descName, obfName, classDescName
            ));
        }
        e.fieldMap.put(descName, obfName);
    }

    /**
     * Copy a parent's class map to a child class.
     *
     * @param parent name of parent class already in the ClassMap
     * @param child  name of child class that should inherit its method/field mappings
     */
    public void addInheritance(String parent, String child) {
        addClassMap(child, child);
        for (Map.Entry<String, String> e : getFieldMap(parent).entrySet()) {
            addFieldMap(child, e.getKey(), e.getValue());
        }
        for (Map.Entry<String, String> e : getMethodMap(parent).entrySet()) {
            addMethodMap(child, e.getKey(), e.getValue());
        }
    }

    /**
     * Get the mapping between descriptive and obfuscated class names.
     *
     * @return HashMap of descriptive name -> obfuscated name
     */
    public HashMap<String, String> getClassMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (Entry<String, ClassMapEntry> e : classMap.entrySet()) {
            map.put(e.getKey(), e.getValue().obfName);
        }
        return map;
    }

    /**
     * Get the mapping between descriptive and obfuscated method names for a class.
     *
     * @param classDescName descriptive class name
     * @return HashMap of descriptive name -> obfuscated name
     */
    public HashMap<String, String> getMethodMap(String classDescName) {
        ClassMapEntry e = classMap.get(classDescName);
        return e.methodMap;
    }

    /**
     * Get the mapping between descriptive and obfuscated field names for a class.
     *
     * @param classDescName descriptive class name
     * @return HashMap of descriptive name -> obfuscated name
     */
    public HashMap<String, String> getFieldMap(String classDescName) {
        ClassMapEntry e = classMap.get(classDescName);
        return e.fieldMap;
    }

    void print() {
        for (Entry<String, ClassMapEntry> e : classMap.entrySet()) {
            if (e.getKey().startsWith(DEFAULT_MINECRAFT_PACKAGE)) {
                continue;
            }
            Logger.log(Logger.LOG_CLASS, "class %s -> %s", e.getKey(), e.getValue().obfName);
            for (Entry<String, String> e1 : e.getValue().methodMap.entrySet()) {
                Logger.log(Logger.LOG_METHOD, "method %s -> %s", e1.getKey(), e1.getValue());
            }
            for (Entry<String, String> e1 : e.getValue().fieldMap.entrySet()) {
                Logger.log(Logger.LOG_FIELD, "field %s -> %s", e1.getKey(), e1.getValue());
            }
        }
    }

    void print(PrintStream out, String indent) {
        for (Entry<String, ClassMapEntry> e : classMap.entrySet()) {
            if (e.getKey().startsWith(DEFAULT_MINECRAFT_PACKAGE)) {
                continue;
            }
            out.printf("%1$sclass %2$s -> %3$s\n", indent, e.getKey(), e.getValue().obfName);
            for (Entry<String, String> e1 : e.getValue().methodMap.entrySet()) {
                out.printf("%1$s%1$smethod %2$s -> %3$s\n", indent, e1.getKey(), e1.getValue());
            }
            for (Entry<String, String> e1 : e.getValue().fieldMap.entrySet()) {
                out.printf("%1$s%1$sfield %2$s -> %3$s\n", indent, e1.getKey(), e1.getValue());
            }
        }
    }

    void apply(ClassFile cf) throws BadBytecode {
        String oldClass = cf.getName();
        ConstPool cp = cf.getConstPool();
        ClassMapEntry classEntry = classMap.get(oldClass);

        if (classEntry != null) {
            cf.renameClass(cf.getName(), classEntry.obfName);
        }

        for (Object o : cf.getMethods()) {
            MethodInfo mi = (MethodInfo) o;

            String oldType = mi.getDescriptor();
            String newType = mapTypeString(oldType);
            if (!oldType.equals(newType)) {
                Logger.log(Logger.LOG_METHOD, "method signature %s -> %s", oldType, newType);
                mi.setDescriptor(newType);
            }

            if (classEntry != null) {
                for (Entry<String, String> e : classEntry.methodMap.entrySet()) {
                    if (e.getKey().equals(mi.getName())) {
                        Logger.log(Logger.LOG_METHOD, "method %s -> %s", mi.getName(), e.getValue());
                        mi.setName(e.getValue());
                    }
                }
            }
        }

        for (Object o : cf.getFields()) {
            FieldInfo fi = (FieldInfo) o;

            String oldType = fi.getDescriptor();
            String newType = mapTypeString(oldType);
            if (!oldType.equals(newType)) {
                Logger.log(Logger.LOG_METHOD, "field signature %s -> %s", oldType, newType);
                fi.setDescriptor(newType);
            }

            if (classEntry != null) {
                for (Entry<String, String> e : classEntry.fieldMap.entrySet()) {
                    if (e.getKey().equals(fi.getName())) {
                        Logger.log(Logger.LOG_METHOD, "field %s -> %s", fi.getName(), e.getValue());
                        fi.setName(e.getValue());
                    }
                }
            }
        }

        int origSize = cp.getSize();
        for (int i = 1; i < origSize; i++) {
            String oldName;
            String oldType;

            switch (cp.getTag(i)) {
                case ConstPool.CONST_Fieldref:
                    oldClass = cp.getFieldrefClassName(i);
                    oldName = cp.getFieldrefName(i);
                    oldType = cp.getFieldrefType(i);
                    break;
                case ConstPool.CONST_Methodref:
                    oldClass = cp.getMethodrefClassName(i);
                    oldName = cp.getMethodrefName(i);
                    oldType = cp.getMethodrefType(i);
                    break;
                default:
                    continue;
            }

            final boolean field = (cp.getTag(i) == ConstPool.CONST_Fieldref);
            String newClass = oldClass;
            String newName = oldName;
            String newType;
            if (classMap.containsKey(oldClass)) {
                ClassMapEntry entry = classMap.get(oldClass);
                newClass = entry.obfName;
                HashMap<String, String> map = (field ? entry.fieldMap : entry.methodMap);
                if (map.containsKey(oldName)) {
                    newName = map.get(oldName);
                }
            }
            newType = mapTypeString(oldType);

            if (oldClass.equals(newClass) && oldName.equals(newName) && oldType.equals(newType)) {
                continue;
            }

            final String oldClass2 = oldClass;
            final String oldName2 = oldName;
            final String oldType2 = oldType;
            final String newClass2 = newClass;
            final String newName2 = newName;
            final String newType2 = newType;
            ClassMod mod = new ClassMod() {
                @Override
                public String getDeobfClass() {
                    return newClass2;
                }
            };
            BytecodePatch patch = new BytecodePatch() {
                private String typeStr;
                private byte[] opcodes;
                private Object oldRef;
                private Object newRef;

                {
                    if (field) {
                        typeStr = "field";
                        opcodes = new byte[]{(byte) GETFIELD, (byte) GETSTATIC, (byte) PUTFIELD, (byte) PUTSTATIC};
                        oldRef = new FieldRef(oldClass2, oldName2, oldType2);
                        newRef = new FieldRef(newClass2, newName2, newType2);
                    } else {
                        typeStr = "method";
                        opcodes = new byte[]{(byte) INVOKEVIRTUAL, (byte) INVOKESTATIC, (byte) INVOKESPECIAL};
                        oldRef = new MethodRef(oldClass2, oldName2, oldType2);
                        newRef = new MethodRef(newClass2, newName2, newType2);
                    }
                }

                public String getDescription() {
                    return String.format("%s ref %s.%s %s -> %s.%s %s",
                        typeStr,
                        oldClass2, oldName2, oldType2,
                        newClass2, newName2, newType2);
                }

                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.build(
                        BinaryRegex.capture(BinaryRegex.subset(opcodes, true)),
                        ConstPoolUtils.reference(methodInfo.getConstPool(), oldRef, false)
                    );
                }

                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ConstPoolUtils.reference(methodInfo.getConstPool(), newRef, true)
                    );
                }
            };
            patch.setClassMod(mod);

            for (Object o : cf.getMethods()) {
                MethodInfo mi = (MethodInfo) o;
                patch.apply(mi);
            }
        }
    }

    /**
     * Maps a class, method, or field reference to obfuscated names.
     *
     * @param javaRef input reference using descriptive names
     * @return JavaRef mapped reference
     * @see ClassRef
     * @see MethodRef
     * @see InterfaceMethodRef
     * @see FieldRef
     */
    public JavaRef map(JavaRef javaRef) {
        if (javaRef instanceof MethodRef) {
            return map((MethodRef) javaRef);
        } else if (javaRef instanceof InterfaceMethodRef) {
            return map((InterfaceMethodRef) javaRef);
        } else if (javaRef instanceof FieldRef) {
            return map((FieldRef) javaRef);
        } else if (javaRef instanceof ClassRef) {
            return map((ClassRef) javaRef);
        } else {
            return javaRef;
        }
    }

    private MethodRef map(MethodRef methodRef) {
        String oldClass = methodRef.getClassName();
        String oldName = methodRef.getName();
        String oldType = methodRef.getType();
        String newClass = oldClass;
        String newName = oldName;
        String newType = mapTypeString(oldType);

        if (classMap.containsKey(oldClass)) {
            ClassMapEntry entry = classMap.get(oldClass);
            newClass = entry.obfName;
            HashMap<String, String> map = entry.methodMap;
            if (map.containsKey(oldName)) {
                newName = map.get(oldName);
            }
        }

        return new MethodRef(newClass, newName, newType);
    }

    private InterfaceMethodRef map(InterfaceMethodRef methodRef) {
        String oldClass = methodRef.getClassName();
        String oldName = methodRef.getName();
        String oldType = methodRef.getType();
        String newClass = oldClass;
        String newName = oldName;
        String newType = mapTypeString(oldType);

        if (classMap.containsKey(oldClass)) {
            ClassMapEntry entry = classMap.get(oldClass);
            newClass = entry.obfName;
            HashMap<String, String> map = entry.methodMap;
            if (map.containsKey(oldName)) {
                newName = map.get(oldName);
            }
        }

        return new InterfaceMethodRef(newClass, newName, newType);
    }

    private FieldRef map(FieldRef fieldRef) {
        String oldClass = fieldRef.getClassName();
        String oldName = fieldRef.getName();
        String oldType = fieldRef.getType();
        String newClass = oldClass;
        String newName = oldName;
        String newType = mapTypeString(oldType);

        if (classMap.containsKey(oldClass)) {
            ClassMapEntry entry = classMap.get(oldClass);
            newClass = entry.obfName;
            HashMap<String, String> map = entry.fieldMap;
            if (map.containsKey(oldName)) {
                newName = map.get(oldName);
            }
        }

        return new FieldRef(newClass, newName, newType);
    }

    private ClassRef map(ClassRef classRef) {
        String oldClass = classRef.getClassName();
        String newClass = oldClass;

        if (classMap.containsKey(oldClass)) {
            newClass = classMap.get(oldClass).obfName;
        }

        return new ClassRef(newClass);
    }

    /**
     * Maps a Java type descriptor.  Can be used for both fields and methods.
     * <p/>
     * e.g.,
     * LStillWater; -> Lrb;
     * ([ILMinecraft;)V -> ([ILnet/minecraft/client/Minecraft;)V
     *
     * @param old Java type descriptor using descriptive class names
     * @return mapped Java type descriptor
     */
    public String mapTypeString(String old) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < old.length(); i++) {
            char c = old.charAt(i);
            if (c == 'L') {
                int end = old.indexOf(';', i);
                String oldType = old.substring(i + 1, end).replaceAll("/", ".");
                String newType = oldType;
                if (classMap.containsKey(oldType)) {
                    newType = classMap.get(oldType).obfName;
                }
                sb.append('L');
                sb.append(newType.replaceAll("\\.", "/"));
                sb.append(';');
                i = end;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    void stringReplace(ClassFile cf, JarOutputStream jar) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        cf.write(new DataOutputStream(baos));
        byte[] data = baos.toByteArray();

        for (Entry<String, ClassMapEntry> e : classMap.entrySet()) {
            if (e.getKey().equals(e.getValue().obfName)) {
                continue;
            }
            byte[] oldName = Util.marshalString(e.getKey());
            byte[] newName = Util.marshalString(e.getValue().obfName.replaceAll("\\.", "/"));
            BinaryMatcher bm = new BinaryMatcher(BinaryRegex.build(oldName));
            int offset = 0;
            while (bm.match(data, offset)) {
                Logger.log(Logger.LOG_METHOD, "string replace %s -> %s @%d", e.getKey(), e.getValue().obfName, bm.getStart());
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                baos2.write(data, 0, bm.getStart());
                baos2.write(newName);
                baos2.write(data, bm.getEnd(), data.length - bm.getEnd());
                offset = bm.getStart() + newName.length;
                data = baos2.toByteArray();
            }
        }

        jar.write(data);
    }

    void merge(ClassMap from) {
        for (Entry<String, ClassMapEntry> e : from.classMap.entrySet()) {
            addClassMap(e.getKey(), e.getValue().obfName);
            for (Entry<String, String> e1 : e.getValue().methodMap.entrySet()) {
                addMethodMap(e.getKey(), e1.getKey(), e1.getValue());
            }
            for (Entry<String, String> e1 : e.getValue().fieldMap.entrySet()) {
                addFieldMap(e.getKey(), e1.getKey(), e1.getValue());
            }
        }
    }

    private static class ClassMapEntry {
        public String obfName;
        public HashMap<String, String> methodMap = new HashMap<String, String>();
        public HashMap<String, String> fieldMap = new HashMap<String, String>();

        public ClassMapEntry(String obfName) {
            this.obfName = obfName;
        }
    }
}
