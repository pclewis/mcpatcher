package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import org.w3c.dom.Element;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

class MinecraftJar {
    private File origFile;
    private File outputFile;
    private Info info;
    private JarFile origJar;
    private JarOutputStream outputJar;

    private static HashMap<String, String> knownMD5s = new HashMap<String, String>() {
        {
            put("beta-1.6.6", "ce80072464433cd5b05d505aa8ff29d1");

            put("beta-1.7.3", "eae3353fdaa7e10a59b4cb5b45bfa10d");

            put("beta-1.8pre1", "7ce3238b148bb67a3b84cf59b7516f55");
            put("beta-1.8pre2", "bff1cf2e4586012ac8907b8e7945d4c3");
            put("beta-1.8.1", "f8c5a2ccd3bc996792bbe436d8cc08bc");

            put("beta-1.9pre1", "b4d9681a1118949d7753e19c35c61ec7");
            put("beta-1.9pre2", "962d79abeca031b44cf8dac8d4fcabe9");
            put("beta-1.9pre3", "334827dbe9183af6d650b39321a99e21");
            put("beta-1.9pre4", "cae41f3746d3c4c440b2d63a403770e7");
            put("beta-1.9pre5", "6258c4f293b939117efe640eda76dca4");
            put("beta-1.9pre6", "2468205154374afe5f9caaba2ffbf5f8");

            put("rc1", "22d708f84dc44fba200c2a5e4261959c");
            put("rc2pre1", "e8e264bcff34aecbc7ef7f850858c1d6");
            put("rc2", "bd569d20dd3dd898ff4371af9bbe14e1");

            put("11w47a", "2ad75c809570663ec561ca707983a45b");
            put("11w48a", "cd86517284d62a0854234ae12abd019c");
            put("11w49a", "a1f7969b6b546c492fecabfcb8e8525a");
        }
    };

    public MinecraftJar(File file) throws IOException {
        info = new Info(file);
        if (!info.isOk()) {
            throw info.exception;
        }

        if (file.getName().equals("minecraft.jar")) {
            origFile = new File(file.getParent(), "minecraft-" + info.version.getVersionString() + ".jar");
            outputFile = file;
            Info origInfo = new Info(origFile);
            if (origInfo.result == Info.MODDED_JAR && info.result == Info.UNMODDED_JAR) {
                Logger.log(Logger.LOG_JAR, "copying unmodded %s over %s", outputFile.getName(), origFile.getName());
                origFile.delete();
            }
            if (!origFile.exists()) {
                createBackup();
            } else if (origInfo.isOk()) {
                info = origInfo;
            }
        } else {
            origFile = file;
            outputFile = new File(file.getParent(), "minecraft.jar");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeStreams();
        super.finalize();
    }

    public MinecraftVersion getVersion() {
        return info.version;
    }

    public boolean isModded() {
        return info.result == Info.MODDED_JAR;
    }

    public void logVersion() {
        Logger.log(Logger.LOG_MAIN, "Minecraft version is %s (md5 %s)", info.version, info.md5);
        if (info.origMD5 == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: could not determine original md5 sum");
        } else if (info.result == Info.MODDED_JAR) {
            Logger.log(Logger.LOG_MAIN, "WARNING: possibly modded minecraft.jar (orig md5 %s)", info.origMD5);
        }
    }

    static void fixJarNames() {
        File binDir = MCPatcherUtils.getMinecraftPath("bin");
        for (String filename : binDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches("^minecraft-(rc)?[0-9][-_.0-9a-zA-Z]*(pre\\d+)?\\.jar$");
            }
        })) {
            try {
                File oldFile = new File(binDir, filename);
                MinecraftVersion version = Info.extractVersion(oldFile);
                if (version != null) {
                    String oldVersion = version.getOldVersionString();
                    String newVersion = version.getVersionString();
                    File newFile = new File(binDir, "minecraft-" + newVersion + ".jar");
                    boolean renameProfile = false;
                    if (!newFile.exists()) {
                        Logger.log(Logger.LOG_JAR, "Renaming %s to %s", oldFile.getName(), newFile.getName());
                        oldFile.renameTo(newFile);
                        renameProfile = true;
                    } else if (oldVersion.startsWith("rc")) {
                        renameProfile = true;
                    }
                    if (renameProfile) {
                        Config config = MCPatcherUtils.config;
                        String oldProfile = "Minecraft " + oldVersion;
                        String newProfile = "Minecraft " + version.getProfileString();
                        config.renameProfile(oldProfile, newProfile);
                        File oldModDir = MCPatcherUtils.getMinecraftPath("mods", oldVersion);
                        File newModDir = MCPatcherUtils.getMinecraftPath("mods", newVersion);
                        if (oldModDir.isDirectory() && !newModDir.exists()) {
                            oldModDir.renameTo(newModDir);
                            config.rewriteModPaths(oldModDir, newModDir);
                        }
                    }
                }
            } catch (Throwable e) {
            }
        }
    }

    static boolean isGarbageFile(String filename) {
        return filename.startsWith("META-INF") || filename.startsWith("__MACOSX") || filename.endsWith(".DS_Store");
    }

    static boolean isClassFile(String filename) {
        return filename.endsWith(".class") && !isGarbageFile(filename);
    }

    static void setDefaultTexturePack() {
        File input = MCPatcherUtils.getMinecraftPath("options.txt");
        if (!input.exists()) {
            return;
        }
        File output = MCPatcherUtils.getMinecraftPath("options.txt.tmp");
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            br = new BufferedReader(new FileReader(input));
            pw = new PrintWriter(new FileWriter(output));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("skin:")) {
                    line = "skin:Default";
                }
                pw.println(line);
            }
        } catch (IOException e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(br);
            MCPatcherUtils.close(pw);
        }
        try {
            Util.copyFile(output, input);
            output.delete();
        } catch (IOException e) {
            Logger.log(e);
        }
    }

    public void createBackup() throws IOException {
        closeStreams();
        if (outputFile.exists() && !origFile.exists()) {
            Util.copyFile(outputFile, origFile);
        }
    }

    public void restoreBackup() throws IOException {
        closeStreams();
        if (origFile.exists()) {
            Util.copyFile(origFile, outputFile);
        }
    }

    public void setOutputFile(File file) {
        outputFile = file;
        closeStreams();
    }

    public JarFile getInputJar() throws IOException {
        if (origJar == null) {
            origJar = new JarFile(origFile, false);
        }
        return origJar;
    }

    public JarOutputStream getOutputJar() throws IOException {
        if (outputJar == null) {
            outputJar = new JarOutputStream(new FileOutputStream(outputFile));
        }
        return outputJar;
    }

    public File getInputFile() {
        return origFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void checkOutput() throws Exception {
        closeStreams();
        JarFile jar = null;
        try {
            jar = new JarFile(outputFile);
        } finally {
            MCPatcherUtils.close(jar);
        }
    }

    public void closeStreams() {
        MCPatcherUtils.close(origJar);
        MCPatcherUtils.close(outputJar);
        origJar = null;
        outputJar = null;
    }

    public void run() {
        File file = getOutputFile();
        File directory = file.getParentFile();
        StringBuilder cp = new StringBuilder();
        for (String p : new String[]{file.getName(), "lwjgl.jar", "lwjgl_util.jar", "jinput.jar"}) {
            cp.append(directory.getPath());
            cp.append("/");
            cp.append(p);
            cp.append(File.pathSeparatorChar);
        }

        int heapSize = MCPatcherUtils.getInt(Config.TAG_JAVA_HEAP_SIZE, 1024);
        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-cp", cp.toString(),
            "-Djava.library.path=" + new File(directory, "natives").getPath(),
            "-Xmx" + heapSize + "M",
            "-Xms" + Math.min(heapSize, 512) + "M",
            "net.minecraft.client.Minecraft"
        );
        pb.redirectErrorStream(true);
        pb.directory(MCPatcherUtils.getMinecraftPath());

        Logger.log(Logger.LOG_MAIN);
        Logger.log(Logger.LOG_MAIN, "Launching %s", file.getPath());
        StringBuilder sb = new StringBuilder();
        for (String s : pb.command()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (s.contains(" ")) {
                sb.append('"');
                sb.append(s);
                sb.append('"');
            } else {
                sb.append(s);
            }
        }
        Logger.log(Logger.LOG_MAIN, "%s", sb.toString());

        try {
            Process p = pb.start();
            if (p != null) {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = input.readLine()) != null) {
                    MCPatcher.checkInterrupt();
                    Logger.log(Logger.LOG_MAIN, "%s", line);
                }
                p.waitFor();
                if (p.exitValue() != 0) {
                    Logger.log(Logger.LOG_MAIN, "Minecraft exited with status %d", p.exitValue());
                }
            }
        } catch (InterruptedException e) {
        } catch (IOException e) {
            Logger.log(e);
        }
    }

    private static class Info {
        static final int MISSING_JAR = 0;
        static final int IO_ERROR = 1;
        static final int CORRUPT_JAR = 2;
        static final int MODDED_JAR = 3;
        static final int MODDED_OR_UNMODDED_JAR = 4;
        static final int UNMODDED_JAR = 5;

        MinecraftVersion version;
        String md5;
        String origMD5;
        int result;
        IOException exception;

        Info(File minecraftJar) {
            result = initialize(minecraftJar);
            if (!isOk() && exception == null) {
                exception = new IOException("unexpected error opening " + minecraftJar.getPath());
            }
        }

        private int initialize(File minecraftJar) {
            if (!minecraftJar.exists()) {
                exception = new FileNotFoundException(minecraftJar.getPath() + " does not exist");
                return MISSING_JAR;
            }

            md5 = Util.computeMD5(minecraftJar);
            if (md5 == null) {
                exception = new IOException("could not open " + minecraftJar.getPath());
                return IO_ERROR;
            }

            boolean haveMetaInf = false;
            JarFile jar = null;
            try {
                HashSet<String> entries = new HashSet<String>();
                jar = new JarFile(minecraftJar);
                for (ZipEntry entry : Collections.list(jar.entries())) {
                    String name = entry.getName();
                    if (entries.contains(name)) {
                        exception = new ZipException("duplicate zip entry " + name);
                        return CORRUPT_JAR;
                    }
                    if (name.startsWith("META-INF")) {
                        haveMetaInf = true;
                    }
                    entries.add(name);
                }
                version = extractVersion(jar, md5);
            } catch (ZipException e) {
                exception = e;
                return CORRUPT_JAR;
            } catch (IOException e) {
                exception = e;
                return IO_ERROR;
            } finally {
                MCPatcherUtils.close(jar);
            }

            if (version == null) {
                exception = new JarException("Could not determine version of " + minecraftJar.getPath());
                return CORRUPT_JAR;
            }
            origMD5 = getOrigMD5(minecraftJar.getParentFile(), version);
            if (!haveMetaInf) {
                return MODDED_JAR;
            }
            if (origMD5 == null) {
                return MODDED_OR_UNMODDED_JAR;
            }
            if (origMD5.equals(md5)) {
                return UNMODDED_JAR;
            }
            return MODDED_JAR;
        }

        static MinecraftVersion extractVersion(File file) {
            return extractVersion(file, Util.computeMD5(file));
        }

        static MinecraftVersion extractVersion(File file, String md5) {
            if (!file.exists()) {
                return null;
            }
            JarFile jar = null;
            try {
                jar = new JarFile(file);
                return extractVersion(jar, md5);
            } catch (Exception e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(jar);
            }
            return null;
        }

        private static MinecraftVersion extractVersion(JarFile jar, String md5) {
            MinecraftVersion version = null;
            InputStream inputStream = null;
            try {
                ZipEntry entry = jar.getEntry("net/minecraft/client/Minecraft.class");
                if (entry == null) {
                    return null;
                }
                inputStream = jar.getInputStream(entry);
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));
                ConstPool constPool = classFile.getConstPool();
                for (int i = 1; i < constPool.getSize(); i++) {
                    if (constPool.getTag(i) == ConstPool.CONST_String) {
                        String value = constPool.getStringInfo(i);
                        version = MinecraftVersion.parseVersion(value);
                        if (version != null) {
                            if (version.getVersionString().equals("rc1") && md5.equals("e8e264bcff34aecbc7ef7f850858c1d6")) {
                                version = MinecraftVersion.parseVersion("Minecraft RC2 Prerelease 1");
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(inputStream);
            }
            return version;
        }

        private static String getOrigMD5(File binDir, MinecraftVersion version) {
            String md5 = knownMD5s.get(version.getVersionString());
            if (md5 != null) {
                return md5;
            }
            File md5File = new File(binDir, "md5s");
            if (!version.isPrerelease() && md5File.exists()) {
                FileInputStream inputStream = null;
                try {
                    Properties properties = new Properties();
                    inputStream = new FileInputStream(md5File);
                    properties.load(inputStream);
                    return properties.getProperty("minecraft.jar");
                } catch (IOException e) {
                    Logger.log(e);
                } finally {
                    MCPatcherUtils.close(inputStream);
                }
            }
            return null;
        }

        boolean isOk() {
            return result > CORRUPT_JAR;
        }
    }
}
