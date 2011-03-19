package com.pclewis.mcpatcher;

import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.jar.JarFile;

class Util {
    public static final boolean mac;
    public static final boolean windows;
    public static final boolean linux;
    public static final boolean is64bit;

    public static boolean isMac() {
        return mac;
    }

    public static boolean isWindows() {
        return windows;
    }

    public static boolean isLinux() {
        return linux;
    }

    public static boolean is64Bit() {
        return is64bit;
    }

    static {
        String os = System.getProperty("os.name").toLowerCase();
        mac = os != null && os.contains("mac os x");
        windows = os != null && os.contains("windows");
        linux = os != null && os.contains("linux");

        String datamodel = System.getProperty("sun.arch.data.model");  // sun-specific, but gets the arch of the jvm
        String arch = System.getProperty("os.arch");  // generic, but gets the arch of the os, not the jvm (may be a 32-bit jvm on a 64-bit os)
        if (datamodel != null) {
            is64bit = (Integer.parseInt(datamodel) >= 64);
        } else if (arch != null) {
            is64bit = arch.contains("64");
        } else {
            is64bit = false;
        }
    }

    protected static byte b(int value, int index) {
        return (byte) ((value >> (index * 8)) & 0xFF);
    }

    protected static byte[] marshal16(int value) {
        return new byte[]{Util.b(value, 1), Util.b(value, 0)};
    }

    protected static byte[] marshalString(String value) {
        byte[] bytes = value.getBytes();
        int len = bytes.length;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(len + 2);
        try {
            bos.write(marshal16(len));
            bos.write(bytes);
        } catch (IOException e) {
            Logger.log(e);
        }
        return bos.toByteArray();
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int count = input.read(buffer);
            if (count < 0) {
                break;
            }
            output.write(buffer, 0, count);
        }
    }

    public static void copyFile(File input, File output) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(input);
            os = new FileOutputStream(output);
            copyStream(is, os);
        } finally {
            close(is);
            close(os);
        }
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Logger.log(e);
            }
        }
    }

    public static void close(JarFile jar) {
        if (jar != null) {
            try {
                jar.close();
            } catch (IOException e) {
                Logger.log(e);
            }
        }
    }

    public static String computeMD5(File file) {
        String md5 = null;
        FileInputStream input = null;
        ByteArrayOutputStream output = null;
        DigestOutputStream dos = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            input = new FileInputStream(file);
            output = new ByteArrayOutputStream();
            dos = new DigestOutputStream(output, md);
            copyStream(input, dos);
            close(dos);
            md5 = BinaryRegex.binToStr(md.digest()).replaceAll(" ", "");
        } catch (Exception e) {
            Logger.log(e);
        } finally {
            close(input);
            close(dos);
            close(output);
        }
        return md5;
    }
}
