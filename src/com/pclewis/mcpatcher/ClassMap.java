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

/**
 * Contains mapping from descriptive class, method, and field names to their obfuscated
 * names in minecraft.jar.  Each Mod has its own ClassMap that is maintained by MCPatcher.
 */
public class ClassMap {
    private static final String DESCRIPTOR_CHARS = BinaryRegex.subset("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$/.[<>;".getBytes(), true);

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

    /**
     * Convert a Java descriptor to a class name, e.g., [Ljava/lang/String; -> java.lang.String
     *
     * @param descriptor type descriptor
     * @return dotted name of package/class
     */
    public static String descriptorToClassName(String descriptor) {
        return descriptor.replaceFirst("^\\[*L(.*);$", "$1").replace('/', '.');
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
            putEntry(entry);
            if (descName.equals("Minecraft") || descName.equals("MinecraftApplet")) {
                putEntry(new ClassMapEntry("net.minecraft.client." + descName, entry));
            } else if (!descName.contains(".")) {
                putEntry(new ClassMapEntry("net.minecraft.src." + descName, entry));
            }
        }
        String oldName = entry.getObfName();
        if (oldName == null) {
            entry.setObfName(obfName);
        } else if (!oldName.equals(obfName.replace('.', '/'))) {
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
     * @param obfType       obfuscated method descriptor
     * @throws RuntimeException if class mapping does not exist yet.
     */
    public void addMethodMap(String classDescName, String descName, String obfName, String obfType) {
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
        entry.addMethod(descName, obfName, obfType);
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
     * @param obfType       obfuscated field descriptor
     * @throws RuntimeException if class mapping does not exist yet.
     */
    public void addFieldMap(String classDescName, String descName, String obfName, String obfType) {
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
        entry.addField(descName, obfName, obfType);
    }

    /**
     * Add class/field/method mappings.
     * <p/>
     *
     * @param from descriptive reference
     * @param to   obfuscated reference
     */
    public void addMap(JavaRef from, JavaRef to) {
        if (!from.getClass().equals(to.getClass())) {
            throw new IllegalArgumentException(String.format("cannot map %s to %s", from.toString(), to.toString()));
        }
        addClassMap(from.getClassName(), to.getClassName());
        if (from instanceof MethodRef || from instanceof InterfaceMethodRef) {
            addMethodMap(from.getClassName(), from.getName(), to.getName(), to.getType());
            addTypeDescriptorMap(from.getType(), to.getType());
        } else if (from instanceof FieldRef) {
            addFieldMap(from.getClassName(), from.getName(), to.getName(), to.getType());
            addTypeDescriptorMap(from.getType(), to.getType());
        }
    }

    /**
     * Add class mappings based on a pair of type descriptors, e.g., (LClassA;I)LClassB; -> (Lab;I)Lbc;
     * <p/>
     *
     * @param fromType type descriptor using descriptive names
     * @param toType   type descriptor using obfuscated names
     * @throws IllegalArgumentException if descriptors do not match
     */
    public void addTypeDescriptorMap(String fromType, String toType) {
        int i;
        int j;
        int i1;
        int j1;
        for (i = 0, j = 0; i < fromType.length() && j < toType.length(); i = i1 + 1, j = j1 + 1) {
            i1 = i;
            j1 = j;
            if (fromType.charAt(i) == 'L') {
                i1 = fromType.indexOf(';', i);
                j1 = toType.indexOf(';', j);
                if (i1 < 0) {
                    throw new IllegalArgumentException(String.format(
                        "invalid type descriptor %s", fromType
                    ));
                }
                if (j1 < 0) {
                    throw new IllegalArgumentException(String.format(
                        "invalid type descriptor %s", toType
                    ));
                }
                String from = fromType.substring(i + 1, i1).replace('.', '/');
                String to = toType.substring(j + 1, j1).replace('.', '/');
                if (!from.equals(to)) {
                    addClassMap(from, to);
                }
            } else if (fromType.charAt(i) != toType.charAt(j)) {
                break;
            }
        }
        if (i < fromType.length() || j < toType.length()) {
            throw new IllegalArgumentException(String.format(
                "incompatible type descriptors %s and %s", fromType, toType
            ));
        }
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
            parentEntry = new ClassMapEntry(parent);
            putEntry(parentEntry);
        }
        ClassMapEntry childEntry = getEntry(child);
        if (childEntry == null) {
            childEntry = new ClassMapEntry(child, child, parentEntry);
            putEntry(childEntry);
        } else {
            childEntry.setParent(parentEntry);
        }
    }

    public boolean isEmpty() {
        return classMap.isEmpty();
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
     * Get the reverse mapping from obfuscated class names to descriptive class names.
     *
     * @return HashMap of obfuscated name -> descriptive name
     */
    public HashMap<String, String> getReverseClassMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        for (Entry<String, ClassMapEntry> e : classMap.entrySet()) {
            if (e.getValue().aliasFor == null) {
                map.put(e.getValue().getObfName(), e.getKey());
            }
        }
        return map;
    }

    /**
     * Get the mapping between descriptive and obfuscated method names for a class.
     *
     * @param classDescName descriptive class name
     * @return HashMap of descriptive name -> obfuscated name/type
     */
    public HashMap<String, MemberEntry> getMethodMap(String classDescName) {
        ClassMapEntry entry = getEntry(classDescName);
        return entry == null ? new HashMap<String, MemberEntry>() : entry.getMethodMap();
    }

    /**
     * Get the mapping between descriptive and obfuscated field names for a class.
     *
     * @param classDescName descriptive class name
     * @return HashMap of descriptive name -> obfuscated name/type
     */
    public HashMap<String, MemberEntry> getFieldMap(String classDescName) {
        ClassMapEntry entry = getEntry(classDescName);
        return entry == null ? new HashMap<String, MemberEntry>() : entry.getFieldMap();
    }

    void print(final PrintStream out, final String indent) {
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
            out.printf("%1$sclass %2$s\n", indent, e.getValue().toString());
            if (e.getValue().aliasFor != null) {
                continue;
            }

            ArrayList<Entry<String, MemberEntry>> sortedMembers;

            sortedMembers = new ArrayList<Entry<String, MemberEntry>>(e.getValue().getMethodMap().entrySet());
            Collections.sort(sortedMembers, new Comparator<Entry<String, MemberEntry>>() {
                public int compare(Entry<String, MemberEntry> o1, Entry<String, MemberEntry> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
            for (Entry<String, MemberEntry> e1 : sortedMembers) {
                out.printf("%1$s%1$smethod %2$s -> %3$s %4$s\n", indent, e1.getKey(), e1.getValue().name, e1.getValue().type);
            }

            sortedMembers = new ArrayList<Entry<String, MemberEntry>>(e.getValue().getFieldMap().entrySet());
            Collections.sort(sortedMembers, new Comparator<Entry<String, MemberEntry>>() {
                public int compare(Entry<String, MemberEntry> o1, Entry<String, MemberEntry> o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
            for (Entry<String, MemberEntry> e1 : sortedMembers) {
                out.printf("%1$s%1$sfield %2$s -> %3$s %4$s\n", indent, e1.getKey(), e1.getValue().name, e1.getValue().type);
            }
        }
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
                HashMap<String, MemberEntry> map = classEntry.getMethodMap();
                if (map.containsKey(mi.getName())) {
                    String newName = map.get(mi.getName()).name;
                    Logger.log(Logger.LOG_METHOD, "method %s -> %s", mi.getName(), newName);
                    mi.setName(newName);
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
                HashMap<String, MemberEntry> map = classEntry.getFieldMap();
                if (map.containsKey(fi.getName())) {
                    String newName = map.get(fi.getName()).name;
                    Logger.log(Logger.LOG_METHOD, "field %s -> %s", fi.getName(), newName);
                    fi.setName(newName);
                }
            }
        }

        int origSize = cp.getSize();
        for (int i = 1; i < origSize; i++) {
            final int tag = cp.getTag(i);
            String oldName;
            String oldType;

            switch (tag) {
                case ConstPool.CONST_Class:
                    oldClass = cp.getClassInfo(i);
                    oldName = null;
                    oldType = null;
                    break;

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
            String newType = null;
            ClassMapEntry entry = getEntry(oldClass);
            if (entry != null) {
                newClass = entry.getObfName();
                HashMap<String, MemberEntry> map = (tag == ConstPool.CONST_Fieldref ? entry.getFieldMap() : entry.getMethodMap());
                if (map.containsKey(oldName)) {
                    newName = map.get(oldName).name;
                }
            }
            if (oldType != null) {
                newType = mapTypeString(oldType);
            }

            if (oldClass.equals(newClass) &&
                (oldName == null || oldName.equals(newName)) &&
                (oldType == null || oldType.equals(newType))) {
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
            mod.classFile = cf;
            BytecodePatch patch = new BytecodePatch() {
                final private String typeStr;
                final private byte[] opcodes;
                final private JavaRef oldRef;
                final private JavaRef newRef;

                {
                    switch (tag) {
                        case ConstPool.CONST_Class:
                            typeStr = "class";
                            opcodes = ConstPoolUtils.CLASSREF_OPCODES;
                            oldRef = new ClassRef(oldClass2);
                            newRef = new ClassRef(newClass2);
                            break;

                        case ConstPool.CONST_Fieldref:
                            typeStr = "field";
                            opcodes = ConstPoolUtils.FIELDREF_OPCODES;
                            oldRef = new FieldRef(oldClass2, oldName2, oldType2);
                            newRef = new FieldRef(newClass2, newName2, newType2);
                            break;

                        case ConstPool.CONST_Methodref:
                            typeStr = "method";
                            opcodes = ConstPoolUtils.METHODREF_OPCODES;
                            oldRef = new MethodRef(oldClass2, oldName2, oldType2);
                            newRef = new MethodRef(newClass2, newName2, newType2);
                            break;

                        case ConstPool.CONST_InterfaceMethodref:
                            typeStr = "interface method";
                            opcodes = ConstPoolUtils.INTERFACEMETHODREF_OPCODES;
                            oldRef = new InterfaceMethodRef(oldClass2, oldName2, oldType2);
                            newRef = new InterfaceMethodRef(newClass2, newName2, newType2);
                            break;

                        default:
                            throw new AssertionError("Unreachable");
                    }
                }

                @Override
                public String getDescription() {
                    if (tag == ConstPool.CONST_Class) {
                        return String.format("%s ref %s -> %s",
                            typeStr, oldClass2, newClass2
                        );
                    } else {
                        return String.format("%s ref %s.%s %s -> %s.%s %s",
                            typeStr,
                            oldClass2, oldName2, oldType2,
                            newClass2, newName2, newType2
                        );
                    }
                }

                @Override
                public String getMatchExpression() {
                    return BinaryRegex.build(
                        BinaryRegex.capture(BinaryRegex.subset(opcodes, true)),
                        ConstPoolUtils.reference(getMethodInfo().getConstPool(), oldRef, false)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ConstPoolUtils.reference(getMethodInfo().getConstPool(), newRef, true)
                    );
                }
            };
            patch.setClassMod(mod);
            patch.apply(cf);
        }
    }

    /**
     * Maps a class to its obfuscated name.
     *
     * @param className
     * @return obfuscated name
     */
    public String map(String className) {
        ClassMapEntry entry = getEntry(className);
        return entry == null ? className : entry.getObfName();
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
            HashMap<String, MemberEntry> map = entry.getMethodMap();
            if (map.containsKey(oldName)) {
                newName = map.get(oldName).name;
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
            HashMap<String, MemberEntry> map = entry.getMethodMap();
            if (map.containsKey(oldName)) {
                newName = map.get(oldName).name;
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
            HashMap<String, MemberEntry> map = entry.getFieldMap();
            if (map.containsKey(oldName)) {
                newName = map.get(oldName).name;
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
     * Returns whether a mapping exists for a given class.
     *
     * @param className
     * @return true if className is in the class map
     */
    public boolean hasMap(String className) {
        return getEntry(className) != null;
    }

    /**
     * Returns whether a mapping exists for a given class, method, or field.
     *
     * @param javaRef
     * @return true if reference is in the class map
     */
    public boolean hasMap(JavaRef javaRef) {
        if (javaRef instanceof MethodRef) {
            return hasMap((MethodRef) javaRef);
        } else if (javaRef instanceof InterfaceMethodRef) {
            return hasMap((InterfaceMethodRef) javaRef);
        } else if (javaRef instanceof FieldRef) {
            return hasMap((FieldRef) javaRef);
        } else if (javaRef instanceof ClassRef) {
            return hasMap((ClassRef) javaRef);
        } else {
            return false;
        }
    }

    private boolean hasMap(MethodRef methodRef) {
        String oldClass = methodRef.getClassName();
        String oldName = methodRef.getName();

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            HashMap<String, MemberEntry> map = entry.getMethodMap();
            if (map.containsKey(oldName)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMap(InterfaceMethodRef methodRef) {
        String oldClass = methodRef.getClassName();
        String oldName = methodRef.getName();

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            HashMap<String, MemberEntry> map = entry.getMethodMap();
            if (map.containsKey(oldName)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMap(FieldRef fieldRef) {
        String oldClass = fieldRef.getClassName();
        String oldName = fieldRef.getName();

        ClassMapEntry entry = getEntry(oldClass);
        if (entry != null) {
            HashMap<String, MemberEntry> map = entry.getFieldMap();
            if (map.containsKey(oldName)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMap(ClassRef classRef) {
        return getEntry(classRef.getClassName()) != null;
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
        if (old == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < old.length(); i++) {
            char c = old.charAt(i);
            if (c == 'L') {
                int end = old.indexOf(';', i);
                String oldType = old.substring(i + 1, end).replace('/', '.');
                String newType = oldType;
                ClassMapEntry entry = getEntry(oldType);
                if (entry != null && entry.getObfName() != null) {
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
            String oldClass = e.getKey();
            String newClass = e.getValue().getObfName().replace('.', '/');
            data = stringReplace1(data, oldClass, newClass);
            data = stringReplace2(data, oldClass, newClass);
        }

        jar.write(data);
    }

    private byte[] stringReplace1(byte[] data, String oldString, String newString) throws IOException {
        if (oldString.equals(newString)) {
            return data;
        }
        byte[] oldData = Util.marshalString(oldString);
        byte[] newData = Util.marshalString(newString);
        BinaryMatcher bm = new BinaryMatcher(BinaryRegex.build(oldData));
        int offset = 0;
        while (bm.match(data, offset)) {
            Logger.log(Logger.LOG_METHOD, "string replace %s -> %s @%d", oldString, newString, bm.getStart());
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            baos2.write(data, 0, bm.getStart());
            baos2.write(newData);
            baos2.write(data, bm.getEnd(), data.length - bm.getEnd());
            offset = bm.getStart() + newData.length;
            data = baos2.toByteArray();
        }
        return data;
    }

    private byte[] stringReplace2(byte[] data, String oldString, String newString) throws IOException {
        if (oldString.equals(newString)) {
            return data;
        }
        String fastExpr = BinaryRegex.build(
            'L',
            oldString.getBytes(),
            ';'
        );
        BinaryMatcher bmFast = new BinaryMatcher(fastExpr);
        BinaryMatcher bm = new BinaryMatcher(BinaryRegex.build(
            0,
            BinaryRegex.any(),
            BinaryRegex.nonGreedy(BinaryRegex.repeat(DESCRIPTOR_CHARS, 0, 255 - oldString.length())),
            fastExpr
        ));
        int offset = 0;
        while (bmFast.match(data, offset) && bm.match(data, offset)) {
            int start = bm.getStart();
            int length = Util.demarshal(data, bm.getStart(), 2);
            int end = start + length + 2;
            if (end >= data.length || length + 2 < bm.getMatchLength()) {
                offset++;
                continue;
            }
            String oldStringFull;
            try {
                oldStringFull = new String(data, start + 2, length, "UTF-8");
            } catch (Throwable e) {
                Logger.log(e);
                offset++;
                continue;
            }
            String newStringFull = oldStringFull.replaceAll("\\b(L?)" + oldString + "\\b", "$1" + newString);
            if (oldStringFull.equals(newStringFull) || newStringFull.matches(".*net/minecraft.*net/minecraft.*")) {
                offset = end;
            } else {
                Logger.log(Logger.LOG_METHOD, "string replace %s -> %s @%d", oldStringFull, newStringFull, start);
                byte[] newData = Util.marshalString(newStringFull);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(data, 0, bm.getStart());
                baos.write(newData);
                baos.write(data, end, data.length - end);
                offset = bm.getStart() + newData.length;
                data = baos.toByteArray();
            }
        }
        return data;
    }

    void merge(ClassMap from) {
        for (Entry<String, ClassMapEntry> e : from.classMap.entrySet()) {
            merge(e.getValue());
        }
    }

    private ClassMapEntry merge(ClassMapEntry entry) {
        ClassMapEntry newEntry = classMap.get(entry.descName);
        if (newEntry != null) {
            if (newEntry.obfName == null && entry.obfName != null) {
                newEntry.setObfName(entry.obfName);
            }
        } else if (entry.aliasFor != null) {
            newEntry = new ClassMapEntry(entry.descName, merge(entry.aliasFor));
        } else if (entry.parent != null) {
            newEntry = new ClassMapEntry(entry.descName, entry.obfName, merge(entry.parent));
        } else {
            newEntry = new ClassMapEntry(entry.descName, entry.obfName);
        }
        for (ClassMapEntry iface : entry.interfaces) {
            newEntry.addInterface(merge(iface));
        }
        newEntry.methodMap.putAll(entry.methodMap);
        newEntry.fieldMap.putAll(entry.fieldMap);
        putEntry(newEntry);
        return newEntry;
    }

    static class MemberEntry {
        String name;
        String type;

        MemberEntry(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof MemberEntry)) {
                return false;
            }
            MemberEntry that = (MemberEntry) o;
            return this.name.equals(that.name) && this.type.equals(that.type);
        }
    }

    private static class ClassMapEntry {
        private String descName = null;
        private String obfName = null;
        private HashMap<String, MemberEntry> methodMap = new HashMap<String, MemberEntry>();
        private HashMap<String, MemberEntry> fieldMap = new HashMap<String, MemberEntry>();
        private ClassMapEntry parent = null;
        private ArrayList<ClassMapEntry> interfaces = new ArrayList<ClassMapEntry>();
        private ClassMapEntry aliasFor = null;

        private ClassMapEntry(String descName) {
            this.descName = descName.replace('.', '/');
        }

        ClassMapEntry(String descName, String obfName) {
            this(descName);
            this.obfName = obfName.replace('.', '/');
        }

        ClassMapEntry(String descName, ClassMapEntry aliasFor) {
            this(descName);
            this.aliasFor = aliasFor;
        }

        ClassMapEntry(String descName, String obfName, ClassMapEntry parent) {
            this(descName, obfName);
            this.parent = parent;
        }

        void setParent(ClassMapEntry parent) {
            this.parent = parent;
        }

        void addInterface(ClassMapEntry iface) {
            interfaces.add(iface);
        }

        void addMethod(String descName, String obfName, String obfType) {
            methodMap.put(descName, new MemberEntry(obfName, obfType));
        }

        void addField(String descName, String obfName, String obfType) {
            fieldMap.put(descName, new MemberEntry(obfName, obfType));
        }

        ClassMapEntry getEntry() {
            return aliasFor == null ? this : aliasFor.getEntry();
        }

        String getObfName() {
            return getEntry().obfName;
        }

        void setObfName(String obfName) {
            getEntry().obfName = obfName.replace('.', '/');
        }

        String getMethod(String descName) {
            MemberEntry member;
            String obfName;
            if (aliasFor != null && (obfName = aliasFor.getMethod(descName)) != null) {
                return obfName;
            }
            if ((member = methodMap.get(descName)) != null) {
                return member.name;
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

        String getField(String descName) {
            MemberEntry member;
            String obfName;
            if (aliasFor != null && (obfName = aliasFor.getField(descName)) != null) {
                return obfName;
            }
            if ((member = fieldMap.get(descName)) != null) {
                return member.name;
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
                sb.append(obfName);
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

        HashMap<String, MemberEntry> getMethodMap() {
            if (aliasFor != null) {
                return aliasFor.getMethodMap();
            }
            HashMap<String, MemberEntry> map = new HashMap<String, MemberEntry>();
            addMethodMap(map);
            return map;
        }

        private void addMethodMap(HashMap<String, MemberEntry> map) {
            for (ClassMapEntry entry : interfaces) {
                entry.addMethodMap(map);
            }
            if (parent != null) {
                parent.addMethodMap(map);
            }
            map.putAll(methodMap);
        }

        HashMap<String, MemberEntry> getFieldMap() {
            if (aliasFor != null) {
                return aliasFor.getFieldMap();
            }
            HashMap<String, MemberEntry> map = new HashMap<String, MemberEntry>();
            addFieldMap(map);
            return map;
        }

        void addFieldMap(HashMap<String, MemberEntry> map) {
            if (parent != null) {
                parent.addFieldMap(map);
            }
            map.putAll(fieldMap);
        }
    }
}
