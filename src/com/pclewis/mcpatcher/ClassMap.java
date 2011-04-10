package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.jar.JarOutputStream;

import static javassist.bytecode.Opcode.*;

/**
 * Contains mapping from descriptive class, method, and field names to their obfuscated
 * names in minecraft.jar.  Each Mod has its own ClassMap that is maintained by MCPatcher.
 */
public class ClassMap {
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
        return filename.replaceAll("\\.class$", "").replaceAll("^/", "").replace('/', '.');
    }

    /**
     * Convert a fully qualified class name into a path to a .class file.
     * e.g., net.minecraft.src.Minecraft -> net/minecraft/src/Minecraft.class
     *
     * @param className dotted name of package/class
     * @return filename
     */
    public static String classNameToFilename(String className) {
        return className.replace('.', '/') + ".class";
    }

    private ClassMapEntry getEntry(String descName) {
        descName = descName.replace('.', '/');
        ClassMapEntry entry = classMap.get(descName);
        return entry == null ? null : entry.getEntry();
    }

    private void putEntry(ClassMapEntry entry) {
        classMap.put(entry.descName, entry);
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
        ClassMapEntry entry = getEntry(descName);
        if (entry == null) {
            entry = new ClassMapEntry(descName, obfName);
            classMap.put(descName, entry);
            if (descName.equals("Minecraft")) {
                putEntry(new ClassMapEntry("net.minecraft.client." + descName, entry));
            } else if (!descName.contains(".")) {
                putEntry(new ClassMapEntry("net.minecraft.src." + descName, entry));
            }
        }
        String oldName = entry.getObfName();
        if (!oldName.equals(obfName.replace('.', '/'))) {
            throw new RuntimeException(String.format(
                "cannot add class map %1$s -> %2$s because there is already a class map for %1$s -> %3$s",
                descName, obfName, oldName
            ));
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
        ClassMapEntry entry = getEntry(classDescName);
        if (entry == null) {
            throw new RuntimeException(String.format(
                "cannot add method map %s.%s -> %s because there is no class map for %s",
                classDescName, descName, obfName, classDescName
            ));
        }
        String oldName = entry.getMethod(descName);
        if (oldName != null && !oldName.equals(obfName)) {
            throw new RuntimeException(String.format(
                "cannot add method map %1$s.%2$s -> %3$s because it is already mapped to %4$s",
                classDescName, descName, obfName, oldName
            ));
        }
        entry.addMethod(descName, obfName);
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
        ClassMapEntry entry = getEntry(classDescName);
        if (entry == null) {
            throw new RuntimeException(String.format(
                "cannot add field map %s.%s -> %s because there is no class map for %s",
                classDescName, descName, obfName, classDescName
            ));
        }
        String oldName = entry.getField(descName);
        if (oldName != null && !oldName.equals(obfName)) {
            throw new RuntimeException(String.format(
                "cannot add field map %1$s.%2$s -> %3$s because it is already mapped to %4$s",
                classDescName, descName, obfName, oldName
            ));
        }
        entry.addField(descName, obfName);
    }

    /**
     * Copy a parent's class map to a child class.
     *
     * @param parent name of parent class already in the ClassMap
     * @param child  name of child class that should inherit its method/field mappings
     */
    public void addInheritance(String parent, String child) {
        ClassMapEntry parentEntry = getEntry(parent);
        if (parentEntry == null) {
            throw new RuntimeException(String.format(
                "cannot add inherited class %s because there is no class map for parent %s",
                child, parent
            ));
        }
        ClassMapEntry childEntry = getEntry(child);
        if (childEntry == null) {
            childEntry = new ClassMapEntry(child, child, parentEntry);
            putEntry(childEntry);
        } else {
            childEntry.setParent(parentEntry);
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
            map.put(e.getKey(), e.getValue().getObfName());
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
        ClassMapEntry entry = getEntry(classDescName);
        return entry == null ? new HashMap<String, String>() : entry.getMethodMap();
    }

    /**
     * Get the mapping between descriptive and obfuscated field names for a class.
     *
     * @param classDescName descriptive class name
     * @return HashMap of descriptive name -> obfuscated name
     */
    public HashMap<String, String> getFieldMap(String classDescName) {
        ClassMapEntry entry = getEntry(classDescName);
        return entry == null ? new HashMap<String, String>() : entry.getFieldMap();
    }

    abstract private class Printer {
        abstract public void logClass(String format, Object... params);

        abstract public void logMethod(String format, Object... params);

        abstract public void logField(String format, Object... params);

        public void print() {
            ArrayList<Entry<String, ClassMapEntry>> sortedClasses = new ArrayList<Entry<String, ClassMapEntry>>(classMap.entrySet());
            Collections.sort(sortedClasses, new Comparator<Entry<String, ClassMapEntry>>() {
                public int compare(Entry<String, ClassMapEntry> o1, Entry<String, ClassMapEntry> o2) {
                    if (o1.getValue().aliasFor == null && o2.getValue().aliasFor != null) {
                        return -1;
                    } else if (o1.getValue().aliasFor != null && o2.getValue().aliasFor == null) {
                        return 1;
                    } else {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                }
            });
            for (Entry<String, ClassMapEntry> e : sortedClasses) {
                logClass("class %s", e.getValue().toString());
                if (e.getValue().aliasFor != null) {
                    continue;
                }

                ArrayList<Entry<String, String>> sortedMembers;

                sortedMembers = new ArrayList<Entry<String, String>>(e.getValue().getMethodMap().entrySet());
                Collections.sort(sortedMembers, new Comparator<Entry<String, String>>() {
                    public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
                for (Entry<String, String> e1 : sortedMembers) {
                    logMethod("method %s -> %s", e1.getKey(), e1.getValue());
                }

                sortedMembers = new ArrayList<Entry<String, String>>(e.getValue().getFieldMap().entrySet());
                Collections.sort(sortedMembers, new Comparator<Entry<String, String>>() {
                    public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
                for (Entry<String, String> e1 : sortedMembers) {
                    logField("field %s -> %s", e1.getKey(), e1.getValue());
                }
            }
        }
    }

    void print() {
        new Printer() {
            @Override
            public void logClass(String format, Object... params) {
                Logger.log(Logger.LOG_CLASS, format, params);
            }

            @Override
            public void logMethod(String format, Object... params) {
                Logger.log(Logger.LOG_METHOD, format, params);
            }

            @Override
            public void logField(String format, Object... params) {
                Logger.log(Logger.LOG_FIELD, format, params);
            }
        }.print();
    }

    void print(final PrintStream out, final String indent) {
        new Printer() {
            private String indent2 = indent + indent;

            @Override
            public void logClass(String format, Object... params) {
                out.printf(indent + format + "\n", params);
            }

            @Override
            public void logMethod(String format, Object... params) {
                out.printf(indent2 + format + "\n", params);
            }

            @Override
            public void logField(String format, Object... params) {
                out.printf(indent2 + format + "\n", params);
            }
        }.print();
    }

    void apply(ClassFile cf) throws BadBytecode {
        String oldClass = cf.getName();
        ConstPool cp = cf.getConstPool();
        ClassMapEntry classEntry = getEntry(oldClass);

        if (classEntry != null) {
            cf.renameClass(cf.getName(), classEntry.getObfName());
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
                for (Entry<String, String> e : classEntry.getMethodMap().entrySet()) {
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
                for (Entry<String, String> e : classEntry.getFieldMap().entrySet()) {
                    if (e.getKey().equals(fi.getName())) {
                        Logger.log(Logger.LOG_METHOD, "field %s -> %s", fi.getName(), e.getValue());
                        fi.setName(e.getValue());
                    }
                }
            }
        }

        int origSize = cp.getSize();
        for (int i = 1; i < origSize; i++) {
            final int tag = cp.getTag(i);
            String oldName;
            String oldType;

            switch (tag) {
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

                case ConstPool.CONST_InterfaceMethodref:
                    oldClass = cp.getInterfaceMethodrefClassName(i);
                    oldName = cp.getInterfaceMethodrefName(i);
                    oldType = cp.getInterfaceMethodrefType(i);
                    break;

                default:
                    continue;
            }

            String newClass = oldClass;
            String newName = oldName;
            String newType;
            ClassMapEntry entry = getEntry(oldClass);
            if (entry != null) {
                newClass = entry.getObfName();
                HashMap<String, String> map = (tag == ConstPool.CONST_Fieldref ? entry.getFieldMap() : entry.getMethodMap());
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
                final private String typeStr;
                final private byte[] opcodes;
                final private JavaRef oldRef;
                final private JavaRef newRef;

                {
                    switch (tag) {
                        case ConstPool.CONST_Fieldref:
                            typeStr = "field";
                            opcodes = new byte[]{(byte) GETFIELD, (byte) GETSTATIC, (byte) PUTFIELD, (byte) PUTSTATIC};
                            oldRef = new FieldRef(oldClass2, oldName2, oldType2);
                            newRef = new FieldRef(newClass2, newName2, newType2);
                            break;

                        case ConstPool.CONST_Methodref:
                            typeStr = "method";
                            opcodes = new byte[]{(byte) INVOKEVIRTUAL, (byte) INVOKESTATIC, (byte) INVOKESPECIAL};
                            oldRef = new MethodRef(oldClass2, oldName2, oldType2);
                            newRef = new MethodRef(newClass2, newName2, newType2);
                            break;

                        case ConstPool.CONST_InterfaceMethodref:
                            typeStr = "interface method";
                            opcodes = new byte[]{(byte) INVOKEINTERFACE};
                            oldRef = new InterfaceMethodRef(oldClass2, oldName2, oldType2);
                            newRef = new InterfaceMethodRef(newClass2, newName2, newType2);
                            break;

                        default:
                            throw new AssertionError("Unreachable");
                    }
                }

                @Override
                public String getDescription() {
                    return String.format("%s ref %s.%s %s -> %s.%s %s",
                        typeStr,
                        oldClass2, oldName2, oldType2,
                        newClass2, newName2, newType2
                    );
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.build(
                        BinaryRegex.capture(BinaryRegex.subset(opcodes, true)),
                        ConstPoolUtils.reference(methodInfo.getConstPool(), oldRef, false)
                    );
                }

                @Override
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

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            newClass = entry.getObfName();
            HashMap<String, String> map = entry.getMethodMap();
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

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            newClass = entry.getObfName();
            HashMap<String, String> map = entry.getMethodMap();
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

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            newClass = entry.getObfName();
            HashMap<String, String> map = entry.getFieldMap();
            if (map.containsKey(oldName)) {
                newName = map.get(oldName);
            }
        }

        return new FieldRef(newClass, newName, newType);
    }

    private ClassRef map(ClassRef classRef) {
        String oldClass = classRef.getClassName();
        String newClass = oldClass;

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            newClass = entry.getObfName();
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
                String oldType = old.substring(i + 1, end).replace('/', '.');
                String newType = oldType;
                ClassMapEntry entry = getEntry(oldType);
                if (entry != null) {
                    newType = entry.getObfName();
                }
                sb.append('L');
                sb.append(newType.replace('.', '/'));
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
            if (e.getKey().equals(e.getValue().getObfName())) {
                continue;
            }
            byte[] oldName = Util.marshalString(e.getKey());
            byte[] newName = Util.marshalString(e.getValue().getObfName().replace('.', '/'));
            BinaryMatcher bm = new BinaryMatcher(BinaryRegex.build(oldName));
            int offset = 0;
            while (bm.match(data, offset)) {
                Logger.log(Logger.LOG_METHOD, "string replace %s -> %s @%d", e.getKey(), e.getValue().getObfName(), bm.getStart());
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
            addClassMap(e.getKey(), e.getValue().getObfName());
            for (Entry<String, String> e1 : e.getValue().getMethodMap().entrySet()) {
                addMethodMap(e.getKey(), e1.getKey(), e1.getValue());
            }
            for (Entry<String, String> e1 : e.getValue().getFieldMap().entrySet()) {
                addFieldMap(e.getKey(), e1.getKey(), e1.getValue());
            }
        }
    }

    private static class ClassMapEntry {
        private String descName = null;
        private String obfName = null;
        private HashMap<String, String> methodMap = new HashMap<String, String>();
        private HashMap<String, String> fieldMap = new HashMap<String, String>();
        private ClassMapEntry parent = null;
        private ArrayList<ClassMapEntry> interfaces = new ArrayList<ClassMapEntry>();
        private ClassMapEntry aliasFor = null;

        private ClassMapEntry(String descName) {
            this.descName = descName.replace('.', '/');
        }

        public ClassMapEntry(String descName, String obfName) {
            this(descName);
            this.obfName = obfName.replace('.', '/');
        }

        public ClassMapEntry(String descName, ClassMapEntry aliasFor) {
            this(descName);
            this.aliasFor = aliasFor;
        }

        public ClassMapEntry(String descName, String obfName, ClassMapEntry parent) {
            this(descName, obfName);
            this.parent = parent;
        }

        public void setParent(ClassMapEntry parent) {
            this.parent = parent;
        }

        public void addInterface(ClassMapEntry parent) {
            interfaces.add(parent);
        }

        public void addMethod(String descName, String obfName) {
            methodMap.put(descName, obfName);
        }

        public void addField(String descName, String obfName) {
            fieldMap.put(descName, obfName);
        }

        public ClassMapEntry getEntry() {
            return aliasFor == null ? this : aliasFor.getEntry();
        }

        public String getObfName() {
            return getEntry().obfName;
        }

        public String getMethod(String descName) {
            String obfName;
            if (aliasFor != null && (obfName = aliasFor.getMethod(descName)) != null) {
                return obfName;
            }
            if ((obfName = methodMap.get(descName)) != null) {
                return obfName;
            }
            if (parent != null && (obfName = parent.getMethod(descName)) != null) {
                return obfName;
            }
            for (ClassMapEntry entry : interfaces) {
                if ((obfName = entry.getMethod(descName)) != null) {
                    return obfName;
                }
            }
            return null;
        }

        public String getField(String descName) {
            String obfName;
            if (aliasFor != null && (obfName = aliasFor.getField(descName)) != null) {
                return obfName;
            }
            if ((obfName = fieldMap.get(descName)) != null) {
                return obfName;
            }
            if (parent != null && (obfName = parent.getField(descName)) != null) {
                return obfName;
            }
            return null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(descName.replace('/', '.'));
            if (obfName != null && !obfName.equals(descName)) {
                sb.append(" (");
                sb.append(obfName.replace('/', '.'));
                sb.append(".class)");
            }
            if (aliasFor != null) {
                sb.append(" alias for ");
                sb.append(aliasFor.descName.replace('/', '.'));
            }
            if (parent != null) {
                sb.append(" extends ");
                sb.append(parent.descName.replace('/', '.'));
            }
            if (!interfaces.isEmpty()) {
                sb.append(" implements");
                for (ClassMapEntry entry : interfaces) {
                    sb.append(' ');
                    sb.append(entry.descName.replace('/', '.'));
                }
            }
            return sb.toString();
        }

        public HashMap<String, String> getMethodMap() {
            if (aliasFor != null) {
                return aliasFor.getMethodMap();
            }
            HashMap<String, String> map = new HashMap<String, String>();
            addMethodMap(map);
            return map;
        }

        private void addMethodMap(HashMap<String, String> map) {
            for (ClassMapEntry entry : interfaces) {
                entry.addMethodMap(map);
            }
            if (parent != null) {
                parent.addMethodMap(map);
            }
            map.putAll(methodMap);
        }

        public HashMap<String, String> getFieldMap() {
            if (aliasFor != null) {
                return aliasFor.getFieldMap();
            }
            HashMap<String, String> map = new HashMap<String, String>();
            addFieldMap(map);
            return map;
        }

        private void addFieldMap(HashMap<String, String> map) {
            if (parent != null) {
                parent.addFieldMap(map);
            }
            map.putAll(fieldMap);
        }
    }
}
