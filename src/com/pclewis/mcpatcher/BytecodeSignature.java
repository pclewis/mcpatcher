package com.pclewis.mcpatcher;

import javassist.bytecode.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassSignature that matches a particular bytecode sequence.
 */
abstract public class BytecodeSignature extends ClassSignature {
    MethodRef deobfMethod;
    ArrayList<String> deobfTypes;
    ArrayList<String> obfTypes;
    private final HashMap<Integer, JavaRef> xrefs = new HashMap<Integer, JavaRef>();

    /**
     * Matcher object.
     *
     * @see BytecodeMatcher
     */
    protected BytecodeMatcher matcher;

    /**
     * Generate a regular expression for the current method.
     *
     * @return String regex
     * @see ClassSignature#push(Object)
     * @see ClassSignature#reference(int, JavaRef)
     */
    abstract public String getMatchExpression();

    void initMatcher() {
        matcher = new BytecodeMatcher(getMatchExpression());
    }

    @Override
    void setClassMod(ClassMod classMod) {
        super.setClassMod(classMod);
        if (deobfMethod != null && deobfMethod.getClassName() == null) {
            deobfMethod = new MethodRef(classMod.getDeobfClass(), deobfMethod.getName(), deobfMethod.getType());
        }
    }

    private boolean filterMethod1() {
        MethodInfo methodInfo = getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        if (codeAttribute == null) {
            return false;
        }
        if (getClassMap().hasMap(deobfMethod)) {
            MethodRef obfTarget = (MethodRef) getClassMap().map(deobfMethod);
            if (!methodInfo.getName().equals(obfTarget.getName())) {
                return false;
            }
        }
        if (deobfMethod != null && deobfMethod.getType() != null) {
            deobfTypes = ConstPoolUtils.parseDescriptor(deobfMethod.getType());
            obfTypes = ConstPoolUtils.parseDescriptor(methodInfo.getDescriptor());
            if (!isPotentialTypeMatch(deobfTypes, obfTypes)) {
                return false;
            }
        }
        return filterMethod();
    }

    private boolean isPotentialTypeMatch(ArrayList<String> deobfTypes, ArrayList<String> obfTypes) {
        if (deobfTypes.size() != obfTypes.size()) {
            return false;
        }
        for (int i = 0; i < deobfTypes.size(); i++) {
            String deobfType = deobfTypes.get(i);
            String obfType = obfTypes.get(i);
            String deobfClass = deobfType.replaceFirst("^\\[+", "");
            String obfClass = obfType.replaceFirst("^\\[+", "");
            if (deobfType.length() - deobfClass.length() != obfType.length() - obfClass.length()) {
                return false;
            }
            if (deobfClass.charAt(0) == 'L' && obfClass.charAt(0) == 'L') {
                deobfClass = ClassMap.descriptorToClassName(deobfClass);
                obfClass = ClassMap.descriptorToClassName(obfClass);
                boolean deobfIsMC = !deobfClass.contains(".") || deobfClass.startsWith("net.minecraft.");
                boolean obfIsMC = !obfClass.matches(".*[^a-z].*") || obfClass.startsWith("net.minecraft.");
                if (deobfIsMC != obfIsMC) {
                    return false;
                } else if (deobfIsMC) {
                    if (classMod.getClassMap().hasMap(deobfClass)) {
                        String deobfMapping = classMod.getClassMap().map(deobfClass).replace('/', '.');
                        if (!deobfMapping.equals(obfClass)) {
                            return false;
                        }
                    }
                } else if (!deobfClass.equals(obfClass)) {
                    return false;
                }
            } else if (!deobfClass.equals(obfClass)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPotentialTypeMatch(String deobfDesc, String obfDesc) {
        return (deobfDesc == null && obfDesc == null) ||
            isPotentialTypeMatch(ConstPoolUtils.parseDescriptor(deobfDesc), ConstPoolUtils.parseDescriptor(obfDesc));
    }

    public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
        boolean matched = false;
        String className = ClassMap.filenameToClassName(filename);
        for (Object o : classFile.getMethods()) {
            MethodInfo methodInfo = (MethodInfo) o;
            classMod.methodInfo = methodInfo;
            if (deobfMethod != null && deobfMethod.getType() != null) {
                deobfTypes = ConstPoolUtils.parseDescriptor(deobfMethod.getType());
                obfTypes = ConstPoolUtils.parseDescriptor(methodInfo.getDescriptor());
            }
            if (match(className, methodInfo, tempClassMap)) {
                matched = true;
                break;
            }
        }
        classMod.methodInfo = null;
        return matched;
    }

    boolean match(String className, MethodInfo methodInfo, ClassMap tempClassMap) {
        if (!filterMethod1()) {
            return false;
        }
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        CodeIterator codeIterator = codeAttribute.iterator();
        initMatcher();
        try {
            for (int offset = 0; offset < codeIterator.getCodeLength() && matcher.match(methodInfo, offset); offset = codeIterator.next()) {
                if (afterMatch1(className, tempClassMap)) {
                    return true;
                }
            }
        } catch (BadBytecode e) {
            Logger.log(e);
        }
        return false;
    }

    private boolean afterMatch1(String className, ClassMap tempClassMap) {
        MethodInfo methodInfo = getMethodInfo();
        ConstPool constPool = methodInfo.getConstPool();
        ArrayList<JavaRef> tempMappings = new ArrayList<JavaRef>();
        for (Map.Entry<Integer, JavaRef> entry : xrefs.entrySet()) {
            int captureGroup = entry.getKey();
            JavaRef xref = entry.getValue();
            byte[] code = matcher.getCaptureGroup(captureGroup);
            int index = Util.demarshal(code, 1, 2);
            ConstPoolUtils.matchOpcodeToRefType(code[0], xref);
            ConstPoolUtils.matchConstPoolTagToRefType(constPool.getTag(index), xref);
            JavaRef newRef = ConstPoolUtils.getRefForIndex(constPool, index);
            if (!isPotentialTypeMatch(xref.getType(), newRef.getType())) {
                if (deobfMethod != null) {
                    Logger.log(Logger.LOG_METHOD, "method %s %s matches %s %s, but",
                        methodInfo.getName(), methodInfo.getDescriptor(), deobfMethod.getName(), deobfMethod.getType()
                    );
                }
                Logger.log(Logger.LOG_METHOD, "method %s %s failed xref #%d %s %s -> %s %s",
                    methodInfo.getName(), methodInfo.getDescriptor(), captureGroup,
                    xref.getName(), xref.getType(), newRef.getName(), newRef.getType()
                );
                return false;
            }
            tempMappings.add(xref);
            tempMappings.add(newRef);
        }
        if (className != null && tempClassMap != null) {
            for (int i = 0; i + 1 < tempMappings.size(); i += 2) {
                tempClassMap.addMap(tempMappings.get(i), tempMappings.get(i + 1));
            }
            if (deobfMethod != null) {
                String deobfName = classMod.getDeobfClass();
                tempClassMap.addClassMap(deobfName, className);
                tempClassMap.addMethodMap(deobfName, deobfMethod.getName(), methodInfo.getName(), methodInfo.getDescriptor());
                if (deobfTypes != null && obfTypes != null) {
                    for (int i = 0; i < deobfTypes.size(); i++) {
                        String desc = ClassMap.descriptorToClassName(deobfTypes.get(i));
                        String obf = ClassMap.descriptorToClassName(obfTypes.get(i));
                        if (!obf.equals(desc)) {
                            tempClassMap.addClassMap(desc, obf);
                        }
                    }
                }
            }
        }
        return afterMatch();
    }

    /**
     * Assigns a name to a signature.  On matching, the target class and method will be added.
     * to the class map.
     *
     * @param methodName descriptive name of method
     * @return this
     */
    public BytecodeSignature setMethodName(String methodName) {
        return setMethod(new MethodRef(null, methodName, null));
    }

    /**
     * Assigns a name to a signature.  On matching, the target class and method will be added.
     * to the class map.
     *
     * @param methodRef descriptive name/type of method
     * @return this
     */
    public BytecodeSignature setMethod(MethodRef methodRef) {
        this.deobfMethod = methodRef;
        return this;
    }

    /**
     * Adds a class cross-reference to a bytecode signature.  After a match, the const pool reference
     * in the capture group will be added to the class map.
     *
     * @param captureGroup matcher capture group
     * @param javaRef      field/method ref using descriptive names
     * @return this
     */
    public BytecodeSignature addXref(int captureGroup, JavaRef javaRef) {
        xrefs.put(captureGroup, javaRef);
        return this;
    }

    /**
     * Called before each method.  Can be overridden to filter out certain methods by signature.
     *
     * @return true if method should be matched
     */
    public boolean filterMethod() {
        return true;
    }

    /**
     * Called immediately after a successful match.  Gives an opportunity to extract bytecode
     * values using getCaptureGroup, for example.
     *
     * @param classFile  matched class file
     * @param methodInfo matched method
     * @see #afterMatch()
     * @deprecated
     */
    public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
    }

    /**
     * Called immediately after a successful match.  Gives an opportunity to extract bytecode
     * values using getCaptureGroup, for example.
     *
     * @return false if match should be ignored
     */
    @SuppressWarnings("deprecation")
    public boolean afterMatch() {
        afterMatch(getClassFile(), getMethodInfo());
        return true;
    }

    /**
     * Get a captured subexpression after a match.  Can only be called in afterMatch.
     *
     * @param group number of capture group, starting at 1
     * @return byte array
     */
    final protected byte[] getCaptureGroup(int group) {
        return matcher.getCaptureGroup(group);
    }

    /**
     * Get matching bytecode string after a match.  Can only be called in afterMatch.
     *
     * @return byte array
     */
    final protected byte[] getMatch() {
        return matcher.getMatch();
    }
}
