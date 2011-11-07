package com.pclewis.mcpatcher;

import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

class Util {
    static int bits;

    static byte b(int value, int index) {
        return (byte) ((value >> (index * 8)) & 0xFF);
    }

    static byte[] marshal16(int value) {
        return new byte[]{Util.b(value, 1), Util.b(value, 0)};
    }

    static byte[] marshalString(String value) {
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

    static int demarshal(byte[] data, int offset, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result <<= 8;
            result |= data[i + offset] & 0xff;
        }
        return result;
    }

    static int demarshal(byte[] data) {
        return demarshal(data, 0, data.length);
    }

    static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int count = input.read(buffer);
            if (count < 0) {
                break;
            }
            output.write(buffer, 0, count);
        }
    }

    static void copyFile(File input, File output) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(input);
            os = new FileOutputStream(output);
            copyStream(is, os);
        } finally {
            MCPatcherUtils.close(is);
            MCPatcherUtils.close(os);
        }
    }

    static boolean contains(byte[] array, int item) {
        byte itemb = (byte) item;
        for (byte b : array) {
            if (itemb == b) {
                return true;
            }
        }
        return false;
    }

    static String computeMD5(File file) {
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
            MCPatcherUtils.close(dos);
            md5 = BinaryRegex.binToStr(md.digest()).replaceAll(" ", "");
        } catch (Exception e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(dos);
            MCPatcherUtils.close(output);
        }
        return md5;
    }

    static void logOSInfo() {
        Logger.log(Logger.LOG_MAIN, "MCPatcher version is %s", MCPatcher.VERSION_STRING);
        Logger.log(Logger.LOG_MAIN, "OS: %s %s %s",
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch")
        );
        String bitString;
        try {
            bits = Integer.parseInt(System.getProperty("sun.arch.data.model"));
            bitString = String.format(" (%d bit)", bits);
        } catch (Throwable e) {
            bits = 0;
            bitString = "";
        }
        Logger.log(Logger.LOG_MAIN, "JVM: %s %s%s",
            System.getProperty("java.vendor"),
            System.getProperty("java.version"),
            bitString
        );
        Logger.log(Logger.LOG_MAIN, "Classpath: %s", System.getProperty("java.class.path"));
    }
}
