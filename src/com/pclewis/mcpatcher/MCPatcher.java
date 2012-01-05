package com.pclewis.mcpatcher;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Main program entry point.
 */
final public class MCPatcher {
    /**
     * Major MCPatcher version number
     */
    public static final int MAJOR_VERSION = 2;
    /**
     * Minor MCPatcher version number
     */
    public static final int MINOR_VERSION = 3;
    /**
     * MCPatcher release number
     */
    public static final int RELEASE_VERSION = 1;
    /**
     * MCPatcher patch level
     */
    public static final int PATCH_VERSION = 0;
    /**
     * MCPatcher beta version if > 0
     */
    public static final int BETA_VERSION = 0;
    /**
     * MCPatcher version as a string
     */
    public static final String VERSION_STRING =
        String.format("%d.%d.%d", MAJOR_VERSION, MINOR_VERSION, RELEASE_VERSION) +
            (PATCH_VERSION > 0 ? String.format("_%02d", PATCH_VERSION) : "") +
            (BETA_VERSION > 0 ? "-beta" + BETA_VERSION : "");

    static MinecraftJar minecraft = null;
    static ModList modList;

    private static boolean ignoreSavedMods = false;
    private static boolean ignoreBuiltInMods = false;
    private static boolean ignoreCustomMods = false;
    private static boolean enableAllMods = false;
    static boolean experimentalMods = false;

    private static UserInterface ui;

    private MCPatcher() {
    }

    /**
     * MCPatcher entry point.
     * <p/>
     * Valid parameters:<br>
     * -version: print version string and exit<br>
     * -loglevel n: set log level to n (0-7)<br>
     * -mcdir path: specify path to minecraft<br>
     * -auto: apply all applicable mods to the default minecraft.jar and exit (no GUI)<br>
     * -ignoresavedmods: do not load mods from mcpatcher.xml<br>
     * -ignorebuiltinmods: do not load mods built into mcpatcher<br>
     * -ignorecustommods: do not load mods from the mcpatcher-mods directory<br>
     * -enableallmods: enable all valid mods instead of selected mods from last time<br>
     * -experimental: load mods considered "experimental"<br>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitStatus = 1;
        boolean guiEnabled = true;
        String enteredMCDir = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-loglevel") && i + 1 < args.length) {
                i++;
                try {
                    Logger.setLogLevel(Integer.parseInt(args[i]));
                } catch (NumberFormatException e) {
                }
            } else if (args[i].equals("-version")) {
                System.out.println(VERSION_STRING);
                System.exit(0);
            } else if (args[i].equals("-mcdir") && i + 1 < args.length) {
                i++;
                enteredMCDir = args[i];
            } else if (args[i].equals("-auto")) {
                guiEnabled = false;
            } else if (args[i].equals("-ignoresavedmods")) {
                ignoreSavedMods = true;
            } else if (args[i].equals("-ignorebuiltinmods")) {
                ignoreBuiltInMods = true;
            } else if (args[i].equals("-ignorecustommods")) {
                ignoreCustomMods = true;
            } else if (args[i].equals("-enableallmods")) {
                enableAllMods = true;
            } else if (args[i].equals("-experimental")) {
                experimentalMods = true;
            }
        }

        if (guiEnabled) {
            ui = new UserInterface.GUI();
        } else {
            ui = new UserInterface.CLI();
        }

        if (!ui.locateMinecraftDir(enteredMCDir)) {
            System.exit(exitStatus);
        }

        MinecraftJar.fixJarNames();
        ui.show();

        Util.logOSInfo();

        if (!MCPatcherUtils.getString(Config.TAG_LAST_VERSION, "").equals(VERSION_STRING)) {
            MCPatcherUtils.set(Config.TAG_LAST_VERSION, VERSION_STRING);
            MCPatcherUtils.set(Config.TAG_BETA_WARNING_SHOWN, false);
            MCPatcherUtils.set(Config.TAG_DEBUG, BETA_VERSION > 0);
            MCPatcherUtils.set(Config.TAG_JAVA_HEAP_SIZE, Util.bits >= 64 ? 1024 : 768);
        }
        if (BETA_VERSION > 0 && !MCPatcherUtils.getBoolean(Config.TAG_BETA_WARNING_SHOWN, false)) {
            ui.showBetaWarning();
            MCPatcherUtils.set(Config.TAG_BETA_WARNING_SHOWN, true);
        }

        if (ui.go()) {
            exitStatus = 0;
        }

        if (ui.shouldExit()) {
            saveProperties();
            System.exit(exitStatus);
        }
    }

    static void saveProperties() {
        if (!ignoreSavedMods && modList != null && MCPatcherUtils.config.selectedProfile != null) {
            modList.updateProperties();
            MCPatcherUtils.config.saveProperties();
        }
    }

    static void checkInterrupt() throws InterruptedException {
        Thread.sleep(0);
    }

    static boolean setMinecraft(File file, boolean createBackup) {
        if (minecraft != null) {
            minecraft.closeStreams();
        }
        if (file == null) {
            minecraft = null;
            return false;
        }
        try {
            minecraft = new MinecraftJar(file);
            if (createBackup) {
                minecraft.createBackup();
            }
            minecraft.logVersion();
            String defaultProfile = Config.getDefaultProfileName(minecraft.getVersion().getProfileString());
            MCPatcherUtils.config.setDefaultProfileName(defaultProfile);
            String selectedProfile = MCPatcherUtils.config.getConfigValue(Config.TAG_SELECTED_PROFILE);
            if (Config.isDefaultProfile(selectedProfile)) {
                MCPatcherUtils.config.selectProfile(defaultProfile);
            } else {
                MCPatcherUtils.config.selectProfile();
            }
            getAllMods();
        } catch (IOException e) {
            minecraft = null;
            Logger.log(e);
            return false;
        }
        return true;
    }

    static void getAllMods() {
        if (modList != null) {
            modList.close();
        }
        modList = new ModList();
        if (!ignoreSavedMods) {
            modList.loadSavedMods();
        }
        if (!ignoreBuiltInMods) {
            modList.loadBuiltInMods();
        }
        if (!ignoreCustomMods) {
            modList.loadCustomMods(MCPatcherUtils.getMinecraftPath("mcpatcher-mods"));
        }
        ui.setModList(modList);
    }

    static void getApplicableMods() throws IOException, InterruptedException {
        JarFile origJar = minecraft.getInputJar();
        for (Mod mod : modList.getAll()) {
            mod.setRefs();
        }

        mapModClasses(origJar);
        mapModClassMembers(origJar);
        resolveModDependencies();
        printModList();

        modList.enableValidMods(enableAllMods);
    }

    private static void mapModClasses(JarFile origJar) throws IOException, InterruptedException {
        int totalFiles = origJar.size();
        Logger.log(Logger.LOG_JAR);
        Logger.log(Logger.LOG_JAR, "Analyzing %s (%d files)", origJar.getName(), totalFiles);

        int procFiles = 0;
        for (JarEntry entry : Collections.list(origJar.entries())) {
            ui.updateProgress(++procFiles, origJar.size());
            String name = entry.getName();

            if (MinecraftJar.isClassFile(name)) {
                ClassFile classFile = new ClassFile(new DataInputStream(origJar.getInputStream(entry)));

                for (Mod mod : modList.getAll()) {
                    for (ClassMod classMod : mod.getClassMods()) {
                        try {
                            if (classMod.matchClassFile(name, classFile)) {
                                checkInterrupt();
                                if (!classMod.global) {
                                    Logger.log(Logger.LOG_CLASS, "%s matches %s", classMod.getDeobfClass(), name);
                                    for (Map.Entry<String, ClassMap.MemberEntry> e : mod.classMap.getMethodMap(classMod.getDeobfClass()).entrySet()) {
                                        Logger.log(Logger.LOG_METHOD, "%s matches %s %s", e.getKey(), e.getValue().name, e.getValue().type);
                                    }
                                }
                                for (ClassSignature cs : classMod.classSignatures) {
                                    classMod.addToConstPool = false;
                                    cs.afterMatch(classFile);
                                }
                            }
                        } catch (InterruptedException e) {
                            throw e;
                        } catch (Throwable e) {
                            classMod.addError(e.toString());
                            Logger.log(e);
                        }
                    }
                }
            }
        }

        for (Mod mod : modList.getAll()) {
            for (ClassMod classMod : mod.getClassMods()) {
                if (!classMod.global && classMod.okToApply()) {
                    checkInterrupt();
                    if (classMod.targetClasses.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : classMod.targetClasses) {
                            sb.append(" ");
                            sb.append(s);
                        }
                        classMod.addError(String.format("multiple classes matched:%s", sb.toString()));
                        Logger.log(Logger.LOG_MOD, "multiple classes matched %s:%s", classMod.getDeobfClass(), sb.toString());
                    } else if (classMod.targetClasses.size() == 0) {
                        classMod.addError("no classes matched");
                        Logger.log(Logger.LOG_MOD, "no classes matched %s", classMod.getDeobfClass());
                    }
                }
            }
        }
    }

    private static void mapModClassMembers(JarFile origJar) throws IOException, InterruptedException {
        Logger.log(Logger.LOG_JAR);
        Logger.log(Logger.LOG_JAR, "Analyzing %s (second pass)", origJar.getName());
        for (Mod mod : modList.getAll()) {
            for (ClassMod classMod : mod.getClassMods()) {
                checkInterrupt();
                try {
                    if (!classMod.global && classMod.okToApply()) {
                        String name = ClassMap.classNameToFilename(classMod.targetClasses.get(0));
                        Logger.log(Logger.LOG_CLASS, "%s (%s)", classMod.getDeobfClass(), name);
                        ClassFile classFile = new ClassFile(new DataInputStream(origJar.getInputStream(new ZipEntry(name))));
                        classMod.addToConstPool = false;
                        classMod.mapClassMembers(name, classFile);
                    }
                } catch (Throwable e) {
                    classMod.addError(e.toString());
                    Logger.log(e);
                }
            }
        }
    }

    private static void resolveModDependencies() throws IOException, InterruptedException {
        boolean didSomething = true;
        while (didSomething) {
            didSomething = false;
            for (Mod mod : modList.getAll()) {
                if (mod.okToApply()) {
                    for (Mod.Dependency dep : mod.dependencies) {
                        Mod dmod = modList.get(dep.name);
                        checkInterrupt();
                        if (dmod == null || !dmod.okToApply()) {
                            mod.addError(String.format("requires %s, which cannot be applied", dep.name));
                            didSomething = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void printModList() {
        Logger.log(Logger.LOG_MAIN);
        Logger.log(Logger.LOG_MAIN, "%d available mods:", modList.getVisible().size());
        for (Mod mod : modList.getVisible()) {
            Logger.log(Logger.LOG_MAIN, "[%3s] %s %s - %s", (mod.okToApply() ? "YES" : "NO"), mod.getName(), mod.getVersion(), mod.getDescription());
            for (ClassMod cm : mod.classMods) {
                if (cm.okToApply()) {
                } else if (cm.targetClasses.size() == 0) {
                    Logger.log(Logger.LOG_MOD, "no classes matched %s", cm.getDeobfClass());
                } else if (cm.targetClasses.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : cm.targetClasses) {
                        sb.append(" ");
                        sb.append(s);
                    }
                    Logger.log(Logger.LOG_MOD, "multiple classes matched %s:%s", cm.getDeobfClass(), sb.toString());
                }
            }
        }
    }

    static void showClassMaps(PrintStream out) {
        if (minecraft == null) {
            out.println("No minecraft jar selected.");
            out.println("Click Browse to choose the input file.");
            out.println();
        } else {
            for (Mod mod : modList.getVisible()) {
                if (!mod.getClassMap().getClassMap().isEmpty()) {
                    out.printf("%s\n", mod.getName());
                    mod.getClassMap().print(out, "    ");
                    out.println();
                }
            }
        }
    }

    static void showPatchResults(PrintStream out) {
        if (modList == null || !modList.isApplied()) {
            out.println("No patches applied yet.");
            out.println("Click Patch on the Mods tab.");
            out.println();
        } else {
            for (Mod mod : modList.getSelected()) {
                if (mod.getClassMods().size() == 0 && mod.filesAdded.isEmpty()) {
                    continue;
                }
                out.printf("%s\n", mod.getName());
                for (Map.Entry<String, String> entry : mod.filesAdded.entrySet()) {
                    out.printf("    %s %s\n", entry.getValue(), entry.getKey());
                }
                for (String name : mod.filesToAdd) {
                    if (!mod.filesAdded.containsKey(name)) {
                        out.printf("    WARNING: %s not added (possible conflict)\n", name);
                    }
                }
                for (ClassMod classMod : mod.getClassMods()) {
                    ArrayList<String> tc = classMod.targetClasses;
                    out.printf("    %s", classMod.getDeobfClass());
                    if (tc.size() == 0) {
                    } else if (tc.size() == 1) {
                        out.printf(" (%s)", ClassMap.classNameToFilename(tc.get(0)));
                    } else {
                        out.print(" (multiple matches:");
                        for (String s : tc) {
                            out.print(' ');
                            out.print(s);
                        }
                        out.print(")");
                    }
                    out.println();
                    ArrayList<Map.Entry<String, Integer>> sortedList = new ArrayList<Map.Entry<String, Integer>>();
                    for (int i = 0; i < classMod.patches.size(); i++) {
                        ClassPatch classPatch = classMod.patches.get(i);
                        if (classPatch.numMatches.isEmpty()) {
                            String desc = null;
                            Throwable e = null;
                            try {
                                desc = classPatch.getDescription();
                            } catch (Throwable e1) {
                                e = e1;
                            }
                            if (desc == null) {
                                desc = String.format("patch %d (%s)", i, e == null ? "no description" : e.getMessage());
                            }
                            final String desc2 = desc;
                            sortedList.add(new Map.Entry<String, Integer>() {
                                public String getKey() {
                                    return desc2;
                                }

                                public Integer getValue() {
                                    return 0;
                                }

                                public Integer setValue(Integer value) {
                                    return value;
                                }
                            });
                        } else {
                            sortedList.addAll(classPatch.numMatches.entrySet());
                        }
                    }
                    Collections.sort(sortedList, new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                    });
                    for (Map.Entry<String, Integer> e : sortedList) {
                        out.printf("        [%d] %s\n", e.getValue(), e.getKey());
                    }
                }
                out.println();
            }
        }
    }

    static HashMap<String, ArrayList<Mod>> getConflicts() {
        HashMap<String, ArrayList<Mod>> conflicts = new HashMap<String, ArrayList<Mod>>();
        ArrayList<Mod> mods = modList.getSelected();
        for (Mod mod : mods) {
            for (String filename : mod.filesToAdd) {
                ArrayList<Mod> modArray = conflicts.get(filename);
                if (modArray == null) {
                    modArray = new ArrayList<Mod>();
                    conflicts.put(filename, modArray);
                }
                modArray.add(mod);
            }
        }
        ArrayList<String> entriesToRemove = new ArrayList<String>();
        for (Map.Entry<String, ArrayList<Mod>> entry : conflicts.entrySet()) {
            if (entry.getValue().size() <= 1) {
                entriesToRemove.add(entry.getKey());
            }
        }
        for (String filename : entriesToRemove) {
            conflicts.remove(filename);
        }
        return conflicts;
    }

    static boolean patch() {
        modList.setApplied(true);
        boolean patchOk = false;
        try {
            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Patching...");

            for (Mod mod : modList.getAll()) {
                mod.resetCounts();
            }

            applyMods();
            minecraft.checkOutput();
            minecraft.closeStreams();

            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Done!");
            patchOk = true;
        } catch (Throwable e) {
            Logger.log(e);
            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Restoring original minecraft.jar due to previous error");
            try {
                minecraft.restoreBackup();
            } catch (IOException e1) {
                Logger.log(e1);
            }
        }
        return patchOk;
    }

    private static void applyMods() throws Exception {
        JarFile origJar = minecraft.getInputJar();
        JarOutputStream outputJar = minecraft.getOutputJar();

        int procFiles = 0;
        for (JarEntry entry : Collections.list(origJar.entries())) {
            checkInterrupt();
            ui.updateProgress(++procFiles, origJar.size());
            String name = entry.getName();
            boolean patched = false;

            if (MinecraftJar.isGarbageFile(name)) {
                continue;
            }
            if (entry.isDirectory()) {
                outputJar.putNextEntry(new ZipEntry(name));
                outputJar.closeEntry();
                continue;
            }

            Mod fromMod = null;
            for (Mod mod : modList.getSelected()) {
                if (mod.filesToAdd.contains(name)) {
                    fromMod = mod;
                }
            }

            InputStream inputStream;
            if (fromMod == null) {
                inputStream = origJar.getInputStream(entry);
            } else {
                inputStream = fromMod.openFile(name);
                if (inputStream == null) {
                    throw new IOException(String.format("could not open %s for %s", name, fromMod.getName()));
                }
                Logger.log(Logger.LOG_MOD, "replacing %s for %s", name, fromMod.getName());
                fromMod.filesAdded.put(name, "replaced");
            }

            if (MinecraftJar.isClassFile(name)) {
                ArrayList<ClassMod> classMods = new ArrayList<ClassMod>();
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));
                String className = ClassMap.filenameToClassName(name);

                for (Mod mod : modList.getSelected()) {
                    int i = modList.indexOf(fromMod);
                    int j = modList.indexOf(mod);
                    if (j > 0 && i > j) {
                        continue;
                    }
                    for (ClassMod classMod : mod.getClassMods()) {
                        if (classMod.targetClasses.contains(className)) {
                            classMods.add(classMod);
                        }
                    }
                }

                patched = applyPatches(name, classFile, classMods);
                if (patched) {
                    outputJar.putNextEntry(new ZipEntry(name));
                    classFile.compact();
                    classFile.write(new DataOutputStream(outputJar));
                    outputJar.closeEntry();
                }
            }

            if (!patched) {
                outputJar.putNextEntry(new ZipEntry(name));
                MCPatcherUtils.close(inputStream);
                if (fromMod == null) {
                    inputStream = origJar.getInputStream(entry);
                } else {
                    inputStream = fromMod.openFile(name);
                }
                Util.copyStream(inputStream, outputJar);
                outputJar.closeEntry();
            }

            MCPatcherUtils.close(inputStream);
        }

        HashMap<String, Mod> allFilesToAdd = new HashMap<String, Mod>();
        for (Mod mod : modList.getSelected()) {
            for (String name : mod.filesToAdd) {
                if (origJar.getEntry(name) == null) {
                    allFilesToAdd.put(name, mod);
                }
            }
        }
        for (Mod mod : modList.getSelected()) {
            for (String name : mod.filesToAdd) {
                if (mod == allFilesToAdd.get(name)) {
                    addFile(mod, name, outputJar);
                }
            }
        }
    }

    private static boolean addFile(Mod mod, String filename, JarOutputStream outputJar) throws IOException, BadBytecode {
        String resource = "/" + filename;
        InputStream inputStream = mod.openFile(resource);
        if (inputStream == null) {
            throw new IOException(String.format("could not open %s for %s", resource, mod.getName()));
        }
        Logger.log(Logger.LOG_CLASS, "adding %s for %s", filename, mod.getName());

        try {
            outputJar.putNextEntry(new ZipEntry(filename));
            ClassMap classMap = mod.classMap;
            if (MinecraftJar.isClassFile(filename) && !classMap.isEmpty()) {
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));
                classMap.apply(classFile);
                classFile.compact();
                classMap.stringReplace(classFile, outputJar);
            } else {
                Util.copyStream(inputStream, outputJar);
            }
            outputJar.closeEntry();
            mod.filesAdded.put(filename, "added");
        } catch (ZipException e) {
            if (!e.toString().contains("duplicate entry")) {
                throw e;
            }
        } finally {
            MCPatcherUtils.close(inputStream);
        }

        return true;
    }

    private static boolean applyPatches(String filename, ClassFile classFile, ArrayList<ClassMod> classMods) throws Exception {
        boolean patched = false;
        for (ClassMod cm : classMods) {
            checkInterrupt();
            if (cm.targetClasses.contains(classFile.getName())) {
                cm.addToConstPool = true;
                cm.prePatch(filename, classFile);
                if (cm.patches.size() > 0) {
                    Logger.log(Logger.LOG_MOD, "applying %s patch to %s for mod %s", cm.getDeobfClass(), filename, cm.mod.getName());
                }
                for (ClassPatch cp : cm.patches) {
                    cp.classMod = cm;
                    if (cp.apply(classFile)) {
                        patched = true;
                    }
                }
                cm.addToConstPool = true;
                cm.postPatch(filename, classFile);
            }
        }
        return patched;
    }
}
