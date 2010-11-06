package com.pclewis.mcpatcher;

import java.io.*;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

public class Util {
	public static final boolean mac;
	public static final boolean windows;
	public static final boolean linux;

	public static boolean isMac() { return mac; }
	public static boolean isWindows() { return windows; }
	public static boolean isLinux() { return linux; }

	static {
		String os = System.getProperty("os.name").toLowerCase();
		mac       = os != null && os.equals("mac os x");
		windows   = os != null && os.indexOf("windows") != -1;
		linux     = os != null && os.indexOf("linux") != -1;
	}

	public static String joinString(Iterable<? extends Object> pColl, String separator) {
		Iterator<? extends Object> oIter;
		if(pColl == null || (!(oIter = pColl.iterator()).hasNext())) return "";
		StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
		while(oIter.hasNext()) oBuilder.append(separator).append(oIter.next());
		return oBuilder.toString();
	}

	public static byte b(int value, int index) {
	    return (byte)((value >> (index*8)) & 0xFF);
	}

	public static byte[] bytes(int value, int n) {
		switch(n) {
			case 1: return new byte[]{b(value, 0)};
			case 2: return new byte[]{b(value, 1), b(value, 0)};
			case 4: return new byte[]{b(value, 3), b(value, 2), b(value, 1), b(value, 0)};
		}
		throw new IllegalArgumentException("n must be 1,2, or 4");
	}

	static void copyStream(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024];
		while(true) {
			int count = input.read(buffer);
			if(count == -1)
				break;
			output.write(buffer, 0, count);
		}
	}

    public static String getMinecraftPath() {
        String home = System.getProperty("user.home", ".");
        if(isWindows()) {
            String appdata = System.getenv("APPDATA");
            return ((appdata != null) ? appdata : home) + "/.minecraft/";
        } else if (isMac()) {
            return home + "/Library/Application Support/minecraft/";
        } else if (isLinux()) {
            return home + "/.minecraft/";
        } else {
            return home + "/minecraft/";
        }
    }

    public static String fileDigest(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md;
        md = MessageDigest.getInstance(algorithm);

        DigestInputStream dis = new DigestInputStream(new FileInputStream(file), md);

        byte[] buffer = new byte[8192];
        while (true) {
            if (dis.read(buffer) == -1)
                break;
        }

        String result = new BigInteger(1, md.digest()).toString(16);
        while (result.length() < md.getDigestLength()*2) {
            result = "0" + result;
        }
        return result;
    }
}
