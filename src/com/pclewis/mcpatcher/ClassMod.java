package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a set of patches to be applied to a class.
 * <p/>
 * During the first "analyzing" phase of MCPatcher, each ClassMod is tested against each class file
 * in the input minecraft jar.  With the exception of ones marked 'global,' each ClassMod must
 * match exactly one class.  A Mod can be applied only if all of its ClassMods have target classes.
 * MCPatcher maintains a mapping of deobfuscated class names to their obfuscated names in minecraft.jar.
 * By convention, a ClassMod subclass should have a name ending in "Mod".  The deobfuscated class name
 * is generated from the name of the ClassMod subclass itself by removing the "Mod" from the end.
 * <p/>
 * During the second analyzing phase, MCPatcher resolves each FieldMapper and MethodMapper object.  These
 * mappings are also stored.  The reason for a second pass is so that all classes are resolved before
 * attempting to map any fields or methods.  For example, the TexturePackList class in Minecraft contains a
 * TexturePackBase field, but it cannot be reliably identified until the obfuscated name of TexturePackBase
 * is known.
 * <p/>
 * During patching, MCPatcher applies each ClassPatch within the ClassMod to the target class file.  These
 * do the work of patching bytecode, adding methods, and making members public.  There are also prePatch and
 * postPatch hooks for doing additional processing not covered by one of the ClassPatch subclasses.
 * <p/>
 * The mapping from deobfuscated names to obfuscated names is stored in a ClassMap object accessible to
 * the ClassMod.
 */
abstract public class ClassMod implements PatchComponent {
    /**
     * Gives access to the Mod object containing this ClassMod.
     */
    protected Mod mod;
    /**
     * List of classes that must be resolved via their ClassMods first.
     */
    protected ArrayList<String> prerequisiteClasses = new ArrayList<String>();
    /**
     * List of signatures that identifies the class file(s) to which the ClassMod should be applied.
     */
    protected ArrayList<ClassSignature> classSignatures = new ArrayList<ClassSignature>();
    /**
     * List of patches to be applied to all matching class files.
     */
    protected ArrayList<ClassPatch> patches = new ArrayList<ClassPatch>();
    /**
     * List of class members to deobfuscate in the target class - not used if global == true.
     */
    protected ArrayList<MemberMapper> memberMappers = new ArrayList<MemberMapper>();
    /**
     * By default, a ClassMod should only match a single class. Set this field to true to allow any number of matches.
     */
    protected boolean global = false;
    /**
     * Descriptive name of parent class if any.
     */
    protected String parentClass;
    /**
     * Descriptive names of interfaces implemented by the class.
     */
    protected String[] interfaces;

    ArrayList<String> targetClasses = new ArrayList<String>();
    ArrayList<String> errors = new ArrayList<String>();
    boolean addToConstPool = false;
    ClassFile classFile;
    MethodInfo methodInfo;
    int bestMatchCount;
    String bestMatch;
    private ArrayList<Label> labels = new ArrayList<Label>();
    private HashMap<String, Integer> labelPositions = new HashMap<String, Integer>();

    boolean matchClassFile(String filename, ClassFile classFile) {
        addToConstPool = false;
        this.classFile = classFile;
        if (!filterFile(filename)) {
            return false;
        }

        ClassMap newMap = new ClassMap();
        String deobfName = getDeobfClass();

        int sigIndex = 0;
        for (ClassSignature cs : classSignatures) {
            boolean found = false;

            if (cs.match(filename, classFile, newMap)) {
                found = true;
            }

            if (found == cs.negate) {
                return false;
            }
            newMap.addClassMap(deobfName, ClassMap.filenameToClassName(filename));
            if (bestMatch == null || sigIndex > bestMatchCount) {
                bestMatch = filename;
                bestMatchCount = sigIndex;
            }
            sigIndex++;
        }

        targetClasses.add(classFile.getName());
        if (targetClasses.size() == 1 && !global) {
            mod.classMap.merge(newMap);
            if (parentClass != null) {
                mod.classMap.addClassMap(parentClass, classFile.getSuperclass());
                mod.classMap.addInheritance(parentClass, deobfName);
            }
            if (interfaces != null) {
                String[] obfInterfaces = classFile.getInterfaces();
                for (int i = 0; i < Math.min(interfaces.length, obfInterfaces.length); i++) {
                    mod.classMap.addClassMap(interfaces[i], obfInterfaces[i]);
                    mod.classMap.addInheritance(interfaces[i], deobfName);
                }
            }
        }

        return true;
    }

    /**
     * Get deobfuscated name of target class.  The default implementation simply strips "Mod" from the end
     * of the ClassMod subclass name itself.
     *
     * @return deobfuscated class name
     */
    public String getDeobfClass() {
        return getClass().getSimpleName().replaceFirst("^_", "").replaceFirst("Mod$", "");
    }

    boolean okToApply() {
        return errors.size() == 0;
    }

    void addError(String error) {
        errors.add(error);
    }

    /**
     * Used to quickly rule out candidate class files based on filename alone.  The default implementation
     * allows only default minecraft classes in the default package or in net.minecraft.client.
     *
     * @param filename full path of .class file within the .jar
     * @return true if a class file should be considered for patching
     */
    protected boolean filterFile(String filename) {
        String className = ClassMap.filenameToClassName(filename);
        if (global) {
            return !className.startsWith("com.jcraft.") && !className.startsWith("paulscode.");
        } else {
            return className.startsWith("net.minecraft.client.") || className.matches("^[a-z]{1,4}$");
        }
    }

    /**
     * Resolves all FieldMapper and MethodMapper objects.
     *
     * @param filename  full path of .class file within the .jar
     * @param classFile current class file
     * @return true if all required mappings were found
     * @throws Exception
     */
    protected boolean mapClassMembers(String filename, ClassFile classFile) throws Exception {
        boolean ok = true;

        for (MemberMapper mapper : memberMappers) {
            String mapperType = mapper.getMapperType();
            if (mapper.descriptor != null) {
                mapper.descriptor = mod.getClassMap().mapTypeString(mapper.descriptor);
            }
            if (mapper instanceof FieldMapper) {
                FieldMapper fm = (FieldMapper) mapper;
                for (Object o : classFile.getFields()) {
                    FieldInfo fi = (FieldInfo) o;
                    if (fm.match(fi)) {
                        String name = fm.getName();
                        if (name != null) {
                            Logger.log(Logger.LOG_METHOD, "%s %s matches %s %s", mapperType, name, fi.getName(), fi.getDescriptor());
                            mod.getClassMap().addFieldMap(getDeobfClass(), name, fi.getName(), fi.getDescriptor());
                        }
                        fm.afterMatch();
                    }
                }
            } else if (mapper instanceof MethodMapper) {
                MethodMapper mm = (MethodMapper) mapper;
                for (Object o : classFile.getMethods()) {
                    MethodInfo mi = (MethodInfo) o;
                    if (mm.match(mi)) {
                        String name = mm.getName();
                        if (name != null) {
                            Logger.log(Logger.LOG_METHOD, "%s %s matches %s %s", mapperType, name, mi.getName(), mi.getDescriptor());
                            mod.getClassMap().addMethodMap(getDeobfClass(), name, mi.getName(), mi.getDescriptor());
                        }
                        mm.afterMatch();
                    }
                }
            } else {
                throw new AssertionError("invalid type");
            }
            if (!mapper.allMatched()) {
                addError(String.format("no match found for %s %s", mapperType, mapper.getName()));
                Logger.log(Logger.LOG_METHOD, "no match found for %s %s", mapperType, mapper.getName());
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Pre-patch hook to do any additional processing on the target class before any ClassPatches
     * are applied.
     *
     * @param filename  full path of .class file within the .jar
     * @param classFile current class file
     * @throws Exception
     */
    public void prePatch(String filename, ClassFile classFile) throws Exception {
    }

    /**
     * Post-patch hook to do any additional processing on the target class after all ClassPatches
     * have been applied.
     *
     * @param filename  full path of .class file within the .jar
     * @param classFile current class file
     * @throws Exception
     */
    public void postPatch(String filename, ClassFile classFile) throws Exception {
    }

    final public static class Label {
        String name;
        boolean save;
        int from;

        Label(String name, boolean save) {
            this.name = name;
            this.save = save;
        }
    }

    final protected Label label(String key) {
        return new Label(key, true);
    }

    final protected Label branch(String key) {
        return new Label(key, false);
    }

    void resetLabels() {
        labels.clear();
        labelPositions.clear();
    }

    void resolveLabels(byte[] code, int start, int labelOffset) {
        for (Map.Entry<String, Integer> e : labelPositions.entrySet()) {
            Logger.log(Logger.LOG_BYTECODE, "label %s -> instruction %d", e.getKey(), start + e.getValue());
        }
        for (Label label : labels) {
            if (!labelPositions.containsKey(label.name)) {
                throw new RuntimeException("no label " + label.name + " defined");
            }
            int to = labelPositions.get(label.name);
            int diff = to - label.from + 1;
            int codepos = label.from + labelOffset;
            Logger.log(Logger.LOG_BYTECODE, "branch offset %s %s -> %+d @%d",
                Mnemonic.OPCODE[code[codepos - 1] & 0xff].toUpperCase(), label.name, diff, label.from - 1 + start
            );
            code[codepos] = Util.b(diff, 1);
            code[codepos + 1] = Util.b(diff, 0);
        }
    }

    // PatchComponent methods

    final public ClassFile getClassFile() {
        return classFile;
    }

    final public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    final public String buildExpression(Object... objects) {
        return BinaryRegex.build(objects);
    }

    final public byte[] buildCode(Object... objects) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        buildCode1(baos, objects);
        return baos.toByteArray();
    }

    private void buildCode1(ByteArrayOutputStream baos, Object[] objects) throws IOException {
        for (Object o : objects) {
            if (o instanceof Byte) {
                baos.write((Byte) o);
            } else if (o instanceof byte[]) {
                baos.write((byte[]) o);
            } else if (o instanceof Integer) {
                baos.write((Integer) o);
            } else if (o instanceof int[]) {
                for (int i : (int[]) o) {
                    baos.write(i);
                }
            } else if (o instanceof Label) {
                Label label = (Label) o;
                if (label.save) {
                    int offset = baos.size();
                    if (labelPositions.containsKey(label.name)) {
                        throw new RuntimeException("label " + label.name + " already defined");
                    }
                    labelPositions.put(label.name, offset);
                } else {
                    label.from = baos.size();
                    labels.add(label);
                    baos.write(0);
                    baos.write(0);
                }
            } else if (o instanceof Object[]) {
                buildCode1(baos, (Object[]) o);
            } else {
                throw new AssertionError("invalid type: " + o.getClass().toString());
            }
        }
    }

    final public Object push(Object value) {
        return ConstPoolUtils.push(getMethodInfo().getConstPool(), value, addToConstPool);
    }

    final public byte[] reference(int opcode, JavaRef ref) {
        return ConstPoolUtils.reference(getMethodInfo().getConstPool(), opcode, map(ref), addToConstPool);
    }

    final public Mod getMod() {
        return mod;
    }

    final public ClassMap getClassMap() {
        return mod.getClassMap();
    }

    final public JavaRef map(JavaRef ref) {
        return mod.getClassMap().map(ref);
    }

    /**
     * Represents a field or method to be located within a class.  By default,
     * the match is done by type signature, but this can be overridden.
     */
    public abstract class MemberMapper {
        /**
         * Deobfuscated member name.
         */
        protected String[] names;
        /**
         * Java type descriptor, e.g.,<br>
         * "[B" represents an array of bytes.<br>
         * "(I)Lnet/minecraft/client/Minecraft;" represents a method taking an int and returning a Minecraft object.
         */
        protected String descriptor;

        private int setAccessFlags;
        private int clearAccessFlags;
        private int count;

        MemberMapper(JavaRef... refs) {
            names = new String[refs.length];
            for (int i = 0; i < refs.length; i++) {
                if (refs[i] != null) {
                    names[i] = refs[i].getName();
                    if (descriptor == null) {
                        descriptor = refs[i].getType();
                    }
                }
            }
            if (descriptor == null) {
                throw new RuntimeException("refs list is empty");
            }
        }

        /**
         * @param names      descriptive field names
         * @param descriptor Java type descriptor
         */
        MemberMapper(String[] names, String descriptor) {
            this.names = names.clone();
            this.descriptor = descriptor;
        }

        /**
         * @param name       descriptive field name
         * @param descriptor Java type descriptor
         */
        MemberMapper(String name, String descriptor) {
            this(new String[]{name}, descriptor);
        }

        /**
         * Specify a required access flag.
         *
         * @param flags access flags
         * @param set   if true, flags are required; if false, flags are forbidden
         * @return this
         * @see AccessFlag
         */
        public MemberMapper accessFlag(int flags, boolean set) {
            if (set) {
                setAccessFlags |= flags;
            } else {
                clearAccessFlags |= flags;
            }
            return this;
        }

        boolean matchInfo(String descriptor, int flags) {
            return descriptor.equals(this.descriptor) &&
                (flags & setAccessFlags) == setAccessFlags &&
                (flags & clearAccessFlags) == 0;
        }

        String getName() {
            return count < names.length ? names[count] : null;
        }

        void afterMatch() {
            count++;
        }

        boolean allMatched() {
            return count >= names.length;
        }

        abstract String getMapperType();
    }

    /**
     * Represents a field to be located within a class.  By default,
     * the match is done by type signature, but this can be overridden.
     */
    public class FieldMapper extends MemberMapper {
        public FieldMapper(FieldRef... refs) {
            super(refs);
        }

        /**
         * @param names      field names
         * @param descriptor field type
         * @see #FieldMapper(FieldRef...)
         * @deprecated
         */
        public FieldMapper(String[] names, String descriptor) {
            super(names, descriptor);
        }

        /**
         * @param name       field name
         * @param descriptor field type
         * @see #FieldMapper(FieldRef...)
         * @deprecated
         */
        public FieldMapper(String name, String descriptor) {
            super(name, descriptor);
        }

        final String getMapperType() {
            return "field";
        }

        /**
         * @param fieldInfo candidate field
         * @return true if fieldInfo matches the desired field
         */
        public boolean match(FieldInfo fieldInfo) {
            return matchInfo(fieldInfo.getDescriptor(), fieldInfo.getAccessFlags());
        }
    }

    /**
     * Represents a method to be located within a class.  By default,
     * the match is done by type signature, but this can be overridden.
     */
    public class MethodMapper extends MemberMapper {
        public MethodMapper(MethodRef... refs) {
            super(refs);
        }

        /**
         * @param names      method names
         * @param descriptor method type
         * @see #MethodMapper(MethodRef...)
         * @deprecated
         */
        public MethodMapper(String[] names, String descriptor) {
            super(names, descriptor);
        }

        /**
         * @param name       method name
         * @param descriptor method type
         * @see #MethodMapper(MethodRef...)
         * @deprecated
         */
        public MethodMapper(String name, String descriptor) {
            super(name, descriptor);
        }

        final String getMapperType() {
            return "method";
        }

        /**
         * @param methodInfo candidate field
         * @return true if fieldInfo matches the desired field
         */
        public boolean match(MethodInfo methodInfo) {
            return matchInfo(methodInfo.getDescriptor(), methodInfo.getAccessFlags());
        }
    }
}
