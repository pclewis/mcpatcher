package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

class MinecraftJar {
    private File origFile;
    private File outputFile;
    private MinecraftVersion version;
    private String md5;
    private String origMD5;
    private JarFile origJar;
    private JarOutputStream outputJar;

    private static HashMap<String, String> knownPrereleases = new HashMap<String, String>() {
        {
            put("1.8pre1", "7ce3238b148bb67a3b84cf59b7516f55");
            put("1.8pre2", "bff1cf2e4586012ac8907b8e7945d4c3");

            put("1.9pre1", "b4d9681a1118949d7753e19c35c61ec7");
            put("1.9pre2", "962d79abeca031b44cf8dac8d4fcabe9");
            put("1.9pre3", "334827dbe9183af6d650b39321a99e21");
            put("1.9pre4", "cae41f3746d3c4c440b2d63a403770e7");
        }
    };

    public MinecraftJar(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + " does not exist");
        }

        fixJarNames();

        version = extractVersion(file);
        if (version == null) {
            throw new IOException("Could not determine version of " + file.getPath());
        }

        checkForDuplicateZipEntries(file);

        origMD5 = getOrigMD5(version);

        if (file.getName().equals("minecraft.jar")) {
            String tmpmd5 = Util.computeMD5(file);
            origFile = new File(file.getParent(), "minecraft-" + version + ".jar");
            outputFile = file;
            if (origFile.exists() && origMD5 != null && tmpmd5 != null && origMD5.equals(tmpmd5)) {
                Logger.log(Logger.LOG_JAR, "copying unmodded %s over %s", outputFile.getName(), origFile.getName());
                origFile.delete();
            }
            if (!origFile.exists()) {
                createBackup();
            }
        } else {
            origFile = file;
            outputFile = new File(file.getParent(), "minecraft.jar");
        }

        md5 = Util.computeMD5(origFile);
        if (md5 == null) {
            throw new IOException("Could not compute md5 sum of " + file.getPath());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeStreams();
        super.finalize();
    }

    public MinecraftVersion getVersion() {
        return version;
    }

    public boolean isModded() {
        return md5 != null && origMD5 != null && !origMD5.equalsIgnoreCase(md5) && !version.isPrerelease();
    }

    public void logVersion() {
        Logger.log(Logger.LOG_MAIN, "Minecraft version is %s (md5 %s)", version, md5);
        if (origMD5 == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: could not determine original md5 sum");
        } else if (!origMD5.equals(md5)) {
            Logger.log(Logger.LOG_MAIN, "WARNING: possibly modded minecraft.jar (orig md5 %s)", origMD5);
        }
    }

    private static void fixJarNames() {
        File binDir = MCPatcherUtils.getMinecraftPath("bin");
        for (String filename : binDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches("^minecraft-[0-9][-_.0-9a-zA-Z]+(pre\\d+)?\\.jar$");
            }
        })) {
            try {
                File oldFile = new File(filename);
                MinecraftVersion version = extractVersion(oldFile);
                if (version != null) {
                    File newFile = new File(binDir, "minecraft-" + version.toString() + ".jar");
                    if (!newFile.exists()) {
                        Logger.log(Logger.LOG_JAR, "Renaming %s to %s", oldFile.getName(), newFile.getName());
                        oldFile.renameTo(newFile);
                    }
                }
            } catch (Throwable e) {
            }
        }
    }

    private static void checkForDuplicateZipEntries(File file) throws IOException {
        ZipFile zip = null;
        try {
            HashSet<String> entries = new HashSet<String>();
            zip = new ZipFile(file);
            for (ZipEntry entry : Collections.list(zip.entries())) {
                String name = entry.getName();
                if (entries.contains(name)) {
                    throw new ZipException("duplicate zip entry " + name);
                }
                entries.add(name);
            }
        } finally {
            MCPatcherUtils.close(zip);
        }
    }

    static private MinecraftVersion extractVersion(File file) {
        if (!file.exists()) {
            return null;
        }

        MinecraftVersion version = null;
        JarFile jar = null;
        InputStream is = null;
        try {
            jar = new JarFile(file, false);
            ZipEntry mc = jar.getEntry("net/minecraft/client/Minecraft.class");
            if (mc == null) {
                return null;
            }
            is = jar.getInputStream(mc);
            ClassFile cf = new ClassFile(new DataInputStream(is));
            ConstPool cp = cf.getConstPool();
            for (int i = 1; i < cp.getSize(); i++) {
                if (cp.getTag(i) == ConstPool.CONST_String) {
                    String value = cp.getStringInfo(i);
                    version = MinecraftVersion.parseVersion(value);
                    if (version != null) {
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(is);
            MCPatcherUtils.close(jar);
        }
        return version;
    }

    private static String getOrigMD5(MinecraftVersion version) {
        if (version.isPrerelease()) {
            return knownPrereleases.get(version.toString());
        }
        File md5File = MCPatcherUtils.getMinecraftPath("bin", "md5s");
        if (md5File.exists()) {
            FileInputStream is = null;
            try {
                Properties properties = new Properties();
                is = new FileInputStream(md5File);
                properties.load(is);
                return properties.getProperty("minecraft.jar");
            } catch (IOException e) {
                Logger.log(e);
            } finally {
                MCPatcherUtils.close(is);
            }
        }
        return null;
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
        if (outputFile.exists() && outputFile.getName().equals("minecraft.jar")) {
            String md5 = Util.computeMD5(outputFile);
            if (md5 != null && origMD5 != null && md5.equals(origMD5)) {
                Util.copyFile(outputFile, origFile);
                return;
            }
        }
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
}
