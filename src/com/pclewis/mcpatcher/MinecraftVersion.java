package com.pclewis.mcpatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a Minecraft version number, e.g., 1.8.1 or 1.9pre1.
 */
final public class MinecraftVersion {
    public static final int ALPHA = 1;
    public static final int BETA = 2;
    public static final int RC = 3;
    public static final int FINAL = 4;

    private static final int NOT_PRERELEASE = 9999;

    private static final Pattern pattern = Pattern.compile(
        "Minecraft\\s+(Alpha|Beta|RC)?\\s*v?([0-9][-_.0-9a-zA-Z]*)\\s*((?:Pre\\S*|Beta)\\s*(\\d+)?)?",
        Pattern.CASE_INSENSITIVE
    );

    private String versionNumberOnly;
    private String versionString;
    private String profileString;
    private int[] parsedVersion;
    private int preRelease;
    private boolean weeklyBuild;

    /**
     * Attempt to parse a string into a minecraft version.
     *
     * @param versionString original version string as it appears in-game, e.g., "Minecraft Beta 1.9 Prerelease"
     * @return MinecraftVersion object or null
     */
    public static MinecraftVersion parseVersion(String versionString) {
        Matcher matcher = pattern.matcher(versionString);
        if (matcher.find()) {
            return new MinecraftVersion(matcher);
        } else {
            return null;
        }
    }

    private MinecraftVersion(Matcher matcher) {
        String[] elements = new String[]{"", "", "", ""};
        for (int i = 0; i < elements.length; i++) {
            if (i < matcher.groupCount()) {
                String value = matcher.group(i + 1);
                if (value != null) {
                    elements[i] = value;
                }
            }
        }
        elements[0] = elements[0].toLowerCase();
        versionNumberOnly = elements[1];
        String[] tokens = versionNumberOnly.split("[^0-9a-zA-Z]+");
        parsedVersion = new int[tokens.length + 1];
        if ("alpha".equals(elements[0])) {
            parsedVersion[0] = ALPHA;
        } else if ("beta".equals(elements[0])) {
            parsedVersion[0] = BETA;
        } else if ("rc".equals(elements[0])) {
            parsedVersion[0] = RC;
        } else {
            parsedVersion[0] = FINAL;
        }
        int i;
        for (i = 0; i < tokens.length; i++) {
            try {
                parsedVersion[i + 1] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
            }
        }
        if (elements[2] == null || elements[2].equals("")) {
            preRelease = NOT_PRERELEASE;
        } else if (elements[3] == null || elements[3].equals("")) {
            preRelease = 1;
        } else {
            try {
                preRelease = Integer.parseInt(elements[3]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                preRelease = 1;
            }
        }
        if (preRelease != NOT_PRERELEASE) {
            versionNumberOnly += "pre" + preRelease;
        }
        switch (parsedVersion[0]) {
            case ALPHA:
                profileString = "Alpha ";
                break;

            case BETA:
                profileString = "Beta ";
                break;

            case RC:
                profileString = "RC";
                break;

            default:
                profileString = "";
                break;
        }
        profileString += versionNumberOnly;
        versionString = profileStringToVersionString(profileString);
        if (parsedVersion[0] == RC) {
            versionNumberOnly = versionString;
        }
        if (parsedVersion[0] == FINAL && versionNumberOnly.length() > 1 && !versionNumberOnly.contains(".")) {
            weeklyBuild = true;
            parsedVersion = new int[]{1, 0, 0};
        }
    }

    /**
     * Get release type.
     *
     * @return ALPHA, BETA, or FINAL
     */
    public int getReleaseType() {
        return parsedVersion[0];
    }

    /**
     * Returns true if version in a prerelease/preview.
     *
     * @return true if prerelease
     */
    public boolean isPrerelease() {
        return preRelease != NOT_PRERELEASE || parsedVersion[0] == RC || weeklyBuild;
    }

    /**
     * Gets version as a string, e.g., beta-1.8.1 or 1.0.0.
     *
     * @return version string
     */
    public String getVersionString() {
        return versionString;
    }

    /**
     * Gets default profile name associated with this version, e.g., Beta 1.8.1 or 1.0.0.
     *
     * @return profile name
     */
    public String getProfileString() {
        return profileString;
    }

    String getOldVersionString() {
        return versionNumberOnly;
    }

    static String profileStringToVersionString(String profileString) {
        return profileString
            .replaceFirst("^Alpha ", "alpha-")
            .replaceFirst("^Beta ", "beta-")
            .replaceFirst("^RC", "rc");
    }

    static String versionStringToProfileString(String versionString) {
        return versionString
            .replaceFirst("^alpha-", "Alpha ")
            .replaceFirst("^beta-", "Beta ")
            .replaceFirst("^rc", "RC");
    }

    /**
     * @see #getVersionString()
     */
    public String toString() {
        return getVersionString();
    }

    /**
     * Compare two MinecraftVersions
     *
     * @param that version to compare with
     * @return 0, &lt; 0, or &gt; 0
     */
    public int compareTo(MinecraftVersion that) {
        int[] a = this.parsedVersion;
        int[] b = that.parsedVersion;
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
        return this.preRelease - that.preRelease;
    }
}
