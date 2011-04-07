package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
     * List of signatures that identifies the class file(s) to which the ClassMod should be applied.
     */
    protected ArrayList<ClassSignature> classSignatures = new ArrayList<ClassSignature>();
    /**
     * List of patches to be applied to all matching class files.
     */
    protected ArrayList<ClassPatch> patches = new ArrayList<ClassPatch>();
    /**
     * List of class fields to deobfuscate in the target class - not used if global == true.
     */
    protected ArrayList<FieldMapper> fieldMappers = new ArrayList<FieldMapper>();
    /**
     * List of class methods to deobfuscate in the target class - not used if global == true.
     */
    protected ArrayList<MethodMapper> methodMappers = new ArrayList<MethodMapper>();
    /**
     * By default, a ClassMod should only match a single class. Set this field to true to allow any number of matches.
     */
    protected boolean global = false;

    ArrayList<String> targetClasses = new ArrayList<String>();
    ArrayList<String> errors = new ArrayList<String>();
    boolean addToConstPool = false;

    void setMod(Mod mod) {
        this.mod = mod;
    }

    boolean matchClassFile(String filename, ClassFile classFile) {
        addToConstPool = false;
        if (!filterFile(filename)) {
            return false;
        }

        ClassMap newMap = new ClassMap();
        String deobfName = getDeobfClass();

        for (ClassSignature cs : classSignatures) {
            boolean found = false;

            if (cs instanceof FilenameSignature) {
                FilenameSignature f = (FilenameSignature) cs;
                if (filename.equals(f.getFilename())) {
                    found = true;
                }
            } else if (cs instanceof BytecodeSignature) {
                BytecodeSignature m = (BytecodeSignature) cs;
                for (Object o : classFile.getMethods()) {
                    MethodInfo mi = (MethodInfo) o;
                    CodeAttribute ca = mi.getCodeAttribute();
                    if (ca != null) {
                        if (m.match(mi)) {
                            if (m.getMethodName() != null) {
                                newMap.addClassMap(deobfName, ClassMap.filenameToClassName(filename));
                                newMap.addMethodMap(deobfName, m.getMethodName(), mi.getName());
                            }
                            found = true;
                            break;
                        }
                    }
                }
            } else if (cs instanceof ConstSignature) {
                ConstSignature c = (ConstSignature) cs;
                ConstPool cp = classFile.getConstPool();
                for (int i = 1; i < cp.getSize(); i++) {
                    if (c.match(cp, i)) {
                        found = true;
                        break;
                    }
                }
            }

            if (found == cs.negate) {
                return false;
            }
            newMap.addClassMap(deobfName, ClassMap.filenameToClassName(filename));
        }

        targetClasses.add(classFile.getName());
        if (targetClasses.size() == 1 && !global) {
            mod.classMap.merge(newMap);
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
        return getClass().getSimpleName().replaceFirst("Mod$", "");
    }

    ArrayList<String> getTargetClasses() {
        return targetClasses;
    }

    boolean okToApply() {
        return errors.size() == 0;
    }

    void addError(String error) {
        errors.add(error);
    }

    /**
     * Used to quickly rule out candidate class files based on filename alone.  The default implementation
     * allows only files in the allowedDirs list.
     *
     * @param filename full path of .class file within the .jar
     * @return true if a class file should be considered for patching
     */
    protected boolean filterFile(String filename) {
        String dir = filename.replaceFirst("[^/]+$", "").replaceFirst("/$", "");
        return mod.allowedDirs.contains(dir);
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

        for (FieldMapper fm : fieldMappers) {
            boolean found = false;
            if (fm.descriptor != null) {
                fm.descriptor = mod.getClassMap().mapTypeString(fm.descriptor);
            }
            for (Object o : classFile.getFields()) {
                FieldInfo fi = (FieldInfo) o;
                if (fm.match(fi)) {
                    Logger.log(Logger.LOG_METHOD, "field %s matches %s", fi.getName(), fm.name);
                    mod.getClassMap().addFieldMap(getDeobfClass(), fm.name, fi.getName());
                    found = true;
                }
            }
            if (!found) {
                addError(String.format("no match found for field %s", fm.name));
                Logger.log(Logger.LOG_METHOD, "no match found for field %s", fm.name);
                ok = false;
            }
        }

        for (MethodMapper mm : methodMappers) {
            boolean found = false;
            if (mm.descriptor != null) {
                mm.descriptor = mod.getClassMap().mapTypeString(mm.descriptor);
            }
            for (Object o : classFile.getMethods()) {
                MethodInfo mi = (MethodInfo) o;
                if (mm.match(mi)) {
                    Logger.log(Logger.LOG_METHOD, "method %s matches %s", mi.getName(), mm.name);
                    mod.getClassMap().addMethodMap(getDeobfClass(), mm.name, mi.getName());
                    found = true;
                }
            }
            if (!found) {
                addError(String.format("no match found for method %s", mm.name));
                Logger.log(Logger.LOG_METHOD, "no match found for method %s", mm.name);
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

    // PatchComponent methods

    final public String buildExpression(Object... objects) {
        return BinaryRegex.build(objects);
    }

    final public byte[] buildCode(Object... objects) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
            } else {
                throw new AssertionError("invalid type");
            }
        }
        return baos.toByteArray();
    }

    final public Object push(MethodInfo methodInfo, Object value) {
        return ConstPoolUtils.push(methodInfo.getConstPool(), value, addToConstPool);
    }

    final public byte[] reference(MethodInfo methodInfo, int opcode, JavaRef ref) {
        return ConstPoolUtils.reference(methodInfo.getConstPool(), opcode, map(ref), addToConstPool);
    }

    final public ClassMap getClassMap() {
        return mod.getClassMap();
    }

    final public JavaRef map(JavaRef ref) {
        return mod.getClassMap().map(ref);
    }

    final public void setModParam(String name, Object value) {
        mod.setModParam(name, value);
    }

    final public String getModParam(String name) {
        return mod.getModParam(name);
    }

    final public int getModParamInt(String name) {
        return mod.getModParamInt(name);
    }

    final public boolean getModParamBool(String name) {
        return mod.getModParamBool(name);
    }

    /**
     * Represents a field to be located within a class.  By default, the match is done by type signature,
     * but this can be overridden.
     */
    public class FieldMapper {
        /**
         * Deobfuscated field name.
         */
        protected String name;
        /**
         * Java type descriptor, e.g., "[B" represents an array of bytes.
         */
        protected String descriptor;

        /**
         * @param name       descriptive field name
         * @param descriptor Java type descriptor
         */
        public FieldMapper(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        /**
         * @param fieldInfo candidate field
         * @return true if fieldInfo matches the desired field
         */
        public boolean match(FieldInfo fieldInfo) {
            return fieldInfo.getDescriptor().equals(descriptor);
        }
    }

    /**
     * Represents a method to be located within a class.  By default, the match is done by type signature,
     * but this can be overridden.
     */
    public class MethodMapper {
        /**
         * Deobfuscated field name.
         */
        protected String name;
        /**
         * Java type descriptor, e.g., "(I)Lnet/minecraft/client/Minecraft;" represents a method taking an
         * int and returning a Minecraft object.
         */
        protected String descriptor;

        /**
         * @param name       descriptive method name
         * @param descriptor Java type descriptor
         */
        public MethodMapper(String name, String descriptor) {
            this.name = name;
            this.descriptor = descriptor;
        }

        /**
         * @param methodInfo candidate method
         * @return true if methodInfo matches the desired method
         */
        public boolean match(MethodInfo methodInfo) {
            return methodInfo.getDescriptor().equals(descriptor);
        }
    }
}
