package com.pclewis.mcpatcher;

import java.util.ArrayList;
import java.util.HashMap;
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

    private static final Pattern LONG_PATTERN = Pattern.compile(
        "Minecraft\\s+(Alpha|Beta|RC)?\\s*v?([0-9][-_.0-9a-zA-Z]*)\\s*((?:Pre\\S*|Beta)\\s*(\\d+)?)?",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SHORT_PATTERN = Pattern.compile(
        "^(alpha-|beta-|rc)?([0-9][-_.0-9a-zA-Z]*)(pre(\\d+))?$",
        Pattern.CASE_INSENSITIVE
    );

    private String versionNumberOnly;
    private String versionString;
    private String profileString;
    private int[] parsedVersion;
    private int preRelease;
    private boolean weeklyBuild;

    static final HashMap<String, String> knownMD5s = new HashMap<String, String>();
    private static final ArrayList<MinecraftVersion> versionOrdering = new ArrayList<MinecraftVersion>();

    static {
        try {
            addKnownVersion("beta-1.6.6", "ce80072464433cd5b05d505aa8ff29d1");

            addKnownVersion("beta-1.7.3", "eae3353fdaa7e10a59b4cb5b45bfa10d");

            addKnownVersion("beta-1.8pre1", "7ce3238b148bb67a3b84cf59b7516f55");
            addKnownVersion("beta-1.8pre2", "bff1cf2e4586012ac8907b8e7945d4c3");
            addKnownVersion("beta-1.8.1", "f8c5a2ccd3bc996792bbe436d8cc08bc");

            addKnownVersion("beta-1.9pre1", "b4d9681a1118949d7753e19c35c61ec7");
            addKnownVersion("beta-1.9pre2", "962d79abeca031b44cf8dac8d4fcabe9");
            addKnownVersion("beta-1.9pre3", "334827dbe9183af6d650b39321a99e21");
            addKnownVersion("beta-1.9pre4", "cae41f3746d3c4c440b2d63a403770e7");
            addKnownVersion("beta-1.9pre5", "6258c4f293b939117efe640eda76dca4");
            addKnownVersion("beta-1.9pre6", "2468205154374afe5f9caaba2ffbf5f8");

            addKnownVersion("rc1", "22d708f84dc44fba200c2a5e4261959c");
            addKnownVersion("rc2pre1", "e8e264bcff34aecbc7ef7f850858c1d6");
            addKnownVersion("rc2", "bd569d20dd3dd898ff4371af9bbe14e1");

            addKnownVersion("1.0.0", "3820d222b95d0b8c520d9596a756a6e6");

            addKnownVersion("11w47a", "2ad75c809570663ec561ca707983a45b");
            addKnownVersion("11w48a", "cd86517284d62a0854234ae12abd019c");
            addKnownVersion("11w49a", "a1f7969b6b546c492fecabfcb8e8525a");
            addKnownVersion("11w50a", "8763eb2747d57e2958295bbd06e764b1");

            addKnownVersion("12w01a", "468f1b4022eb81d5ca2f316e24a7ffe5");

            addKnownVersion("1.1", "e92302d2acdba7c97e0d8df1e10d2006");

            addKnownVersion("12w03a", "ea85d9c4058ba9e47d8130bd1bff8be9");
            addKnownVersion("12w04a", "c2e2d8c38288ac122001f2ed11c4d83a");
            addKnownVersion("12w05a", "feabb7967bd528a9f3309a2d660d555d");
            addKnownVersion("12w05b", "70affb4ae7da7e8b24f1bbbcbe58cf0f");
            addKnownVersion("12w06a", "9cfaa4adec02642574ffb7c23a084d74");
            addKnownVersion("12w07a", "d60621a26a64f3bda2849c32da6765c6");
            addKnownVersion("12w07b", "88a9a9055d0d1d17b1c797e280508d83");
            addKnownVersion("12w08a", "1d04d6b190a2ad14d8996802b9286bef");

            addKnownVersion("1.2", "ee18a8cc1db8d15350bceb6ee71292f4");
            addKnownVersion("1.2.2", "6189e96efaea11e5164b4a4755574324");
            addKnownVersion("1.2.3", "12f6c4b1bdcc63f029e3c088a364b8e4");
            addKnownVersion("1.2.4", "25423eab6d8707f96cc6ad8a21a7250a");
            addKnownVersion("1.2.5", "8e8778078a175a33603a585257f28563");

            addKnownVersion("12w15a", "90626a5c36f87aadbc7e79da1f076e93");

            for (int i = 0; i < versionOrdering.size(); i++) {
                MinecraftVersion a = versionOrdering.get(i);
                for (int j = 0; j < versionOrdering.size(); j++) {
                    MinecraftVersion b = versionOrdering.get(j);
                    Integer result = a.comparePartial(b);
                    if (i == j) {
                        if (result == null || result != 0) {
                            throw new RuntimeException("incorrect ordering in known version table: " + a.getVersionString() + " != " + b.getVersionString());
                        }
                    } else if (i > j) {
                        if (result != null && result <= 0) {
                            throw new RuntimeException("incorrect ordering in known version table: " + a.getVersionString() + " <= " + b.getVersionString());
                        }
                    } else {
                        if (result != null && result >= 0) {
                            throw new RuntimeException("incorrect ordering in known version table: " + a.getVersionString() + " >= " + b.getVersionString());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Logger.log(e);
        }
    }

    private static void addKnownVersion(String versionString, String md5) {
        try {
            MinecraftVersion version = parseShortVersion(versionString);
            if (version == null) {
                throw new IllegalArgumentException("bad known version " + version);
            }
            if (!md5.matches("\\p{XDigit}{32}")) {
                throw new IllegalArgumentException("bad md5 sum for known version " + version);
            }
            versionOrdering.add(version);
            knownMD5s.put(versionString, md5);
        } catch (Throwable e) {
            Logger.log(e);
        }
    }

    private static int findClosestKnownVersion(MinecraftVersion version) {
        int i;
        for (i = 0; i < versionOrdering.size(); i++) {
            Integer partialResult = version.comparePartial(versionOrdering.get(i));
            if (partialResult != null && partialResult <= 0) {
                break;
            }
        }
        return i;
    }

    /**
     * Attempt to parse a string into a minecraft version.
     *
     * @param versionString original version string as it appears in-game, e.g., "Minecraft Beta 1.9 Prerelease"
     * @return MinecraftVersion object or null
     */
    public static MinecraftVersion parseVersion(String versionString) {
        Matcher matcher = LONG_PATTERN.matcher(versionString);
        if (matcher.find()) {
            return new MinecraftVersion(matcher);
        } else {
            return null;
        }
    }

    public static MinecraftVersion parseShortVersion(String versionString) {
        Matcher matcher = SHORT_PATTERN.matcher(versionString);
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
        if (elements[2].equals("")) {
            Matcher m = Pattern.compile("(.*)(pre)(\\d*)").matcher(elements[1]);
            if (m.matches()) {
                elements[1] = m.group(1);
                elements[2] = m.group(2);
                elements[3] = m.group(3);
            }
        }
        versionNumberOnly = elements[1];
        String[] tokens = versionNumberOnly.split("[^0-9a-zA-Z]+");
        parsedVersion = new int[tokens.length + 1];
        if (elements[0].startsWith("alpha")) {
            parsedVersion[0] = ALPHA;
        } else if (elements[0].startsWith("beta")) {
            parsedVersion[0] = BETA;
        } else if (elements[0].startsWith("rc")) {
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
            Pattern p = Pattern.compile("^(\\d+)w(\\d+)(.)$");
            Matcher m = p.matcher(versionNumberOnly);
            if (m.matches()) {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                int c = m.group(3).charAt(0) & 0xff;
                parsedVersion = new int[]{FINAL, 1, 0, 0, a, b, c};
            } else {
                parsedVersion = new int[]{FINAL, 1, 0, 0, 1};
            }
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

    private Integer comparePartial(MinecraftVersion that) {
        if (this.weeklyBuild != that.weeklyBuild) {
            return null;
        }
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

    /**
     * Compare two MinecraftVersions.
     *
     * @param that version to compare with
     * @return 0, &lt; 0, or &gt; 0
     */
    public int compareTo(MinecraftVersion that) {
        Integer partialResult = comparePartial(that);
        if (partialResult != null) {
            return partialResult;
        }
        int i = findClosestKnownVersion(this);
        int j = findClosestKnownVersion(that);
        if (i != j) {
            return i - j;
        }
        return this.getVersionString().compareTo(that.getVersionString());
    }

    /**
     * Compare to version string.
     *
     * @param versionString version to compare with
     * @return 0, &lt; 0, or &gt; 0
     */
    public int compareTo(String versionString) {
        if (!versionString.startsWith("Minecraft")) {
            versionString = "Minecraft " + versionString;
        }
        return compareTo(parseVersion(versionString));
    }
}
