package com.pclewis.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public MinecraftJar(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + " does not exist");
        }

        fixPreviewJarName("1.8", "pre1", "7ce3238b148bb67a3b84cf59b7516f55");
        fixPreviewJarName("1.8", "pre2", "bff1cf2e4586012ac8907b8e7945d4c3");
        fixPreviewJarName("1.9", "pre1", "b4d9681a1118949d7753e19c35c61ec7");

        extractVersion(file);
        if (version == null) {
            throw new IOException("Could not determine version of " + file.getPath());
        }

        checkForDuplicateZipEntries(file);

        if (file.getName().equals("minecraft.jar")) {
            origFile = new File(file.getParent(), "minecraft-" + version + ".jar");
            outputFile = file;
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

        origMD5 = getOrigMD5();
    }

    public MinecraftVersion getVersion() {
        return version;
    }

    public boolean isModded() {
        return md5 != null && origMD5 != null && !origMD5.equalsIgnoreCase(md5) && !version.isPreview();
    }

    public void logVersion() {
        Logger.log(Logger.LOG_MAIN, "Minecraft version is %s (md5 %s)", version, md5);
        if (origMD5 == null) {
            Logger.log(Logger.LOG_MAIN, "WARNING: could not determine original md5 sum");
        } else if (!origMD5.equals(md5)) {
            Logger.log(Logger.LOG_MAIN, "WARNING: possibly modded minecraft.jar (orig md5 %s)", origMD5);
        }
    }

    private static void fixPreviewJarName(String version, String pre, String md5) {
        try {
            File jar = MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version + ".jar");
            File jarPre = MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version + pre + ".jar");
            if (jar.exists() && !jarPre.exists()) {
                String jarMD5 = Util.computeMD5(jar);
                if (md5.equals(jarMD5)) {
                    Logger.log(Logger.LOG_JAR, "Renaming %s to %s", jar.getName(), jarPre.getName());
                    jar.renameTo(jarPre);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
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

    private MinecraftVersion extractVersion(File file) {
        if (!file.exists()) {
            return null;
        }

        version = null;
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

    private static String getOrigMD5() {
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

    static boolean isClassFile(String filename) {
        return filename.endsWith(".class") && !filename.startsWith("__MACOSX");
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
