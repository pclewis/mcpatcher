package com.pclewis.mcpatcher;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class MinecraftVersion {
    private static final Pattern pattern = Pattern.compile(
        "Minecraft\\s+(Alpha|Beta)?\\s*v?([0-9][-_.0-9a-zA-Z]+)\\s*((?:Pre\\S*|Beta)\\s*(\\d+)?)?",
        Pattern.CASE_INSENSITIVE
    );

    private String versionString;
    private int[] versionNumbers;

    public static MinecraftVersion parseVersion(String versionString) {
        Matcher matcher = pattern.matcher(versionString);
        if (matcher.find()) {
            return new MinecraftVersion(matcher);
        } else {
            return null;
        }
    }

    private MinecraftVersion(Matcher matcher) {
        String[] elements = new String[]{
            matcher.group(1).toLowerCase(),
            matcher.group(2),
            matcher.group(3),
            matcher.group(4)
        };
        versionString = elements[1];
        String[] tokens = versionString.split("[^0-9a-zA-Z]+");
        versionNumbers = new int[tokens.length + 2];
        if ("alpha".equals(elements[0])) {
            versionNumbers[0] = 1;
        } else if ("beta".equals(elements[0])) {
            versionNumbers[0] = 2;
        } else {
            versionNumbers[0] = 3;
        }
        int i;
        for (i = 0; i < tokens.length; i++) {
            try {
                versionNumbers[i + 1] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
            }
        }
        int prerelease;
        if (elements[2] == null || elements[2].equals("")) {
            prerelease = 0;
        } else if (elements[3] == null || elements[3].equals("")) {
            prerelease = 1;
        } else {
            try {
                prerelease = Integer.parseInt(elements[3]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                prerelease = 1;
            }
        }
        if (prerelease > 0) {
            versionNumbers[i + 1] = -prerelease;
            versionString += "pre" + prerelease;
        }
    }

    public boolean isPreview() {
        return versionNumbers[versionNumbers.length - 1] < 0;
    }

    public String toString() {
        return versionString;
    }

    public int compareTo(MinecraftVersion that) {
        int[] a = this.versionNumbers;
        int[] b = that.versionNumbers;
        int i;
        for (i = 0; i < a.length && i < b.length; i++) {
            if (a[i] != b[i]) {
                return a[i] - b[i];
            }
        }
        if (i < a.length) {
            return a[i];
        }
        if (i < b.length) {
            return -b[i];
        }
        return 0;
    }
}
