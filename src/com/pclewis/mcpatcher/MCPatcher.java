package com.pclewis.mcpatcher;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

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
    public static final int MINOR_VERSION = 0;
    /**
     * MCPatcher patch level
     */
    public static final int PATCH_VERSION = 0;
    /**
     * MCPatcher beta version if > 0
     */
    public static final int BETA_VERSION = 4;
    /**
     * MCPatcher version as a string
     */
    public static final String VERSION_STRING =
        String.format("%d.%d.%d", MAJOR_VERSION, MINOR_VERSION, PATCH_VERSION) +
            (BETA_VERSION > 0 ? "-beta" + BETA_VERSION : "");

    /**
     * Name of utility class always injected into output minecraft.jar
     */
    public static final String UTILS_CLASS = "com.pclewis.mcpatcher.MCPatcherUtils";

    static MinecraftJar minecraft = null;
    static ModList modList;

    private static boolean ignoreBuiltInMods = false;
    private static boolean ignoreCustomMods = false;

    private static MainForm mainForm = null;

    private MCPatcher() {
    }

    /**
     * MCPatcher entry point.
     * <p/>
     * Valid parameters:<br>
     * -loglevel n: set log level to n (0-7)<br>
     * -auto: apply all applicable mods to the default minecraft.jar and exit (no GUI)<br>
     * -ignorebuiltinmods: do not load mods built into mcpatcher<br>
     * -ignorecustommods: do not load mods from the mcpatcher-mods directory
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        boolean guiEnabled = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-loglevel") && i + 1 < args.length) {
                i++;
                try {
                    Logger.setLogLevel(Integer.parseInt(args[i]));
                } catch (NumberFormatException e) {
                }
            } else if (args[i].equals("-auto")) {
                guiEnabled = false;
            } else if (args[i].equals("-ignorebuiltinmods")) {
                ignoreBuiltInMods = true;
            } else if (args[i].equals("-ignorecustommods")) {
                ignoreCustomMods = true;
            }
        }

        if (guiEnabled) {
            mainForm = new MainForm();
            mainForm.show();
        }

        Util.logOSInfo();

        if (!MCPatcherUtils.getString("lastVersion", "").equals(VERSION_STRING)) {
            MCPatcherUtils.set("lastVersion", VERSION_STRING);
            MCPatcherUtils.set("betaWarningShown", false);
        }
        if (guiEnabled && BETA_VERSION > 0 && !MCPatcherUtils.getBoolean("betaWarningShown", false)) {
            mainForm.showBetaDialog();
            MCPatcherUtils.set("betaWarningShown", true);
        }
        MCPatcherUtils.saveProperties();
        getAllMods();

        File minecraftDir = MCPatcherUtils.getMinecraftPath();
        if (!minecraftDir.isDirectory()) {
            Logger.log(Logger.LOG_MAIN, "ERROR: could not find minecraft directory %s", minecraftDir.getPath());
            if (guiEnabled) {
                mainForm.showNoMinecraftDialog(minecraftDir);
            }
            System.exit(1);
        }

        File defaultMinecraft = MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar");
        if (!defaultMinecraft.exists()) {
            if (guiEnabled) {
                mainForm.setBusy(false);
            }
        } else if (setMinecraft(defaultMinecraft, true)) {
            if (guiEnabled) {
                mainForm.updateModList();
            } else {
                int exitStatus = 1;
                try {
                    getApplicableMods();
                    if (patch()) {
                        MCPatcherUtils.saveProperties();
                        exitStatus = 0;
                    }
                    System.out.println();
                    System.out.println("#### Class map:");
                    showClassMaps(System.out);
                    System.out.println("#### Patch summary:");
                    showPatchResults(System.out);
                } catch (Exception e) {
                    Logger.log(e);
                }
                System.exit(exitStatus);
            }
        } else {
            Logger.log(Logger.LOG_MAIN, "ERROR: %s missing or corrupt", defaultMinecraft.getPath());
            if (guiEnabled) {
                mainForm.showCorruptJarError();
                mainForm.setBusy(false);
            }
        }

        if (guiEnabled) {
            mainForm.updateControls();
        }
    }

    static void checkInterrupt() throws InterruptedException {
        Thread.sleep(0);
    }

    static boolean setMinecraft(File file, boolean createBackup) {
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
        } catch (IOException e) {
            minecraft = null;
            Logger.log(e);
            return false;
        }
        return true;
    }

    static void getAllMods() {
        modList = new ModList();
        if (!ignoreBuiltInMods) {
            modList.loadBuiltInMods();
        }
        if (!ignoreCustomMods) {
            modList.loadCustomMods(MCPatcherUtils.getMinecraftPath("mcpatcher-mods"));
        }

        if (mainForm != null) {
            mainForm.setModList(modList);
        }
    }

    static void getApplicableMods() throws IOException, InterruptedException {
        JarFile origJar = minecraft.getInputJar();

        mapModClasses(origJar);
        mapModClassMembers(origJar);
        resolveModDependencies();
        printModList();

        modList.enableValidMods();
    }

    private static void mapModClasses(JarFile origJar) throws IOException, InterruptedException {
        int totalFiles = origJar.size();
        Logger.log(Logger.LOG_JAR);
        Logger.log(Logger.LOG_JAR, "Analyzing %s (%d files)", origJar.getName(), totalFiles);

        int procFiles = 0;
        for (JarEntry entry : Collections.list(origJar.entries())) {
            if (mainForm != null) {
                mainForm.updateProgress(++procFiles, origJar.size());
            }
            String name = entry.getName();

            if (name.endsWith(".class")) {
                ClassFile classFile = new ClassFile(new DataInputStream(origJar.getInputStream(entry)));

                for (Mod mod : modList.getAll()) {
                    for (ClassMod classMod : mod.getClassMods()) {
                        try {
                            if (classMod.matchClassFile(name, classFile)) {
                                checkInterrupt();
                                if (!classMod.global) {
                                    Logger.log(Logger.LOG_CLASS, "%s matches %s.class", name, classMod.getDeobfClass());
                                    for (Map.Entry<String, String> e : mod.classMap.getMethodMap(classMod.getDeobfClass()).entrySet()) {
                                        Logger.log(Logger.LOG_METHOD, "%s matches %s", e.getValue(), e.getKey());
                                    }
                                }
                                for (ClassSignature cs : classMod.classSignatures) {
                                    classMod.addToConstPool = false;
                                    cs.afterMatch(classFile);
                                }
                            }
                        } catch (InterruptedException e) {
                            throw e;
                        } catch (Exception e) {
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
                    if (classMod.getTargetClasses().size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : classMod.targetClasses) {
                            sb.append(" ");
                            sb.append(s);
                        }
                        classMod.addError(String.format("multiple classes matched:%s", sb.toString()));
                        Logger.log(Logger.LOG_MOD, "multiple classes matched %s:%s", classMod.getDeobfClass(), sb.toString());
                    } else if (classMod.getTargetClasses().size() == 0) {
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
                        String name = ClassMap.classNameToFilename(classMod.getTargetClasses().get(0));
                        Logger.log(Logger.LOG_CLASS, "%s (%s)", classMod.getDeobfClass(), name);
                        ClassFile classFile = new ClassFile(new DataInputStream(origJar.getInputStream(new ZipEntry(name))));
                        classMod.addToConstPool = false;
                        classMod.mapClassMembers(name, classFile);
                    }
                } catch (Exception e) {
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
            if (mainForm == null) {
                mod.getClassMap().print();
            }
        }
    }

    static void showClassMaps(PrintStream out) {
        if (minecraft == null) {
            out.println("No minecraft jar selected.");
            out.println("Click Browse to choose the input file.");
        } else {
            for (Mod mod : modList.getVisible()) {
                out.printf("%s\n", mod.getName());
                mod.getClassMap().print(out, "    ");
                out.println();
            }
        }
    }

    static void showPatchResults(PrintStream out) {
        if (modList == null || !modList.isApplied()) {
            out.println("No patches applied yet.");
            out.println("Click Patch on the Mods tab.");
        } else {
            for (Mod mod : modList.getSelected()) {
                out.printf("%s\n", mod.getName());
                for (ClassMod classMod : mod.getClassMods()) {
                    ArrayList<String> tc = classMod.getTargetClasses();
                    out.printf("    %s", classMod.getDeobfClass());
                    if (tc.size() == 0) {
                    } else if (tc.size() == 1) {
                        out.printf(" (%s.class)", tc.get(0));
                    } else {
                        out.print(" (multiple matches:");
                        for (String s : tc) {
                            out.print(' ');
                            out.print(s);
                        }
                        out.print(")");
                    }
                    ArrayList<Map.Entry<String, Integer>> sortedList = new ArrayList<Map.Entry<String, Integer>>();
                    for (ClassPatch classPatch : classMod.patches) {
                        sortedList.addAll(classPatch.numMatches.entrySet());
                    }
                    Collections.sort(sortedList, new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                    });
                    out.println();
                    for (Map.Entry<String, Integer> e : sortedList) {
                        out.printf("        [%d] %s\n", e.getValue(), e.getKey());
                    }
                }
                out.println();
            }
        }
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
        } catch (Exception e) {
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
            if (mainForm != null) {
                mainForm.updateProgress(++procFiles, origJar.size());
            }
            String name = entry.getName();
            boolean patched = false;

            if (name.startsWith("META-INF")) {
                continue; // leave out manifest
            }
            if (entry.isDirectory()) {
                outputJar.putNextEntry(new ZipEntry(name));
                outputJar.closeEntry();
                continue;
            }

            InputStream inputStream = origJar.getInputStream(entry);

            for (Mod mod : modList.getSelected()) {
                for (String f : mod.filesToReplace) {
                    if (name.equals(f)) {
                        if (addOrReplaceFile("replacing", mod, name, outputJar)) {
                            patched = true;
                            break;
                        }
                    }
                }
            }

            if (!patched && name.endsWith(".class")) {
                ArrayList<ClassMod> classMods = new ArrayList<ClassMod>();
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));

                for (Mod mod : modList.getSelected()) {
                    for (ClassMod classMod : mod.getClassMods()) {
                        if (classMod.getTargetClasses().contains(ClassMap.filenameToClassName(name))) {
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
                Util.close(inputStream);
                inputStream = origJar.getInputStream(entry);
                Util.copyStream(inputStream, outputJar);
                outputJar.closeEntry();
            }

            Util.close(inputStream);
        }

        for (Mod mod : modList.getSelected()) {
            for (String name : mod.filesToAdd) {
                if (!addOrReplaceFile("adding", mod, name, outputJar)) {
                    throw new IOException(String.format("could not open %s for %s", name, mod));
                }
            }
        }
    }

    private static boolean addOrReplaceFile(String action, Mod mod, String filename, JarOutputStream outputJar) throws IOException, BadBytecode {
        InputStream inputStream = mod.openFile(filename);
        if (inputStream == null) {
            return false;
        }
        Logger.log(Logger.LOG_CLASS, "%s %s for %s", action, filename, mod.getName());

        try {
            outputJar.putNextEntry(new ZipEntry(filename));
            if (filename.endsWith(".class")) {
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));
                mod.classMap.apply(classFile);
                classFile.compact();
                mod.classMap.stringReplace(classFile, outputJar);
            } else {
                Util.copyStream(inputStream, outputJar);
            }
            outputJar.closeEntry();
        } finally {
            Util.close(inputStream);
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
