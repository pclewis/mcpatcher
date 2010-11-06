package com.pclewis.mcpatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comparable version number, using some subset of the format "variant maj.min.build_rev"
 */
public class Version implements Comparable {
    public static final Pattern PATTERN = Pattern.compile(
            "(?:(\\S+) )?(?:v ?|version ?)?(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?"
    );

    public String variant  = null;
    public int    major    = -1;
    public int    minor    = -1;
    public int    build    = -1;
    public int    revision = -1;

    public Version(String str) {
        Matcher m = PATTERN.matcher(str);
        if(m.find()) {
            variant  = m.group(1);
            if(variant!=null) variant = variant.toLowerCase();
            major    = parseInt(m.group(2));
            minor    = parseInt(m.group(3));
            build    = parseInt(m.group(4));
            revision = parseInt(m.group(5));
        } else {
            throw new IllegalArgumentException("Invalid version:" + str);
        }
    }

    /**
     * Convert a version to a string. Note that the variant is always lowercase,
     * and components never have leading zeroes.
     * @return String representation of version.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(variant != null && variant.length() > 0)
            sb.append(variant).append(" ");
        sb.append(major);
        if(minor > -1) sb.append(".").append(minor);
        if(build > -1) sb.append(".").append(build);
        if(revision > -1) sb.append("_").append(revision);

        return sb.toString();
    }

    private static int parseInt(String str) {
        if(str == null || str.length()==0)
            return -1;
        return Integer.parseInt(str, 10);
    }

    public int compareTo(Object o) {
        if(o instanceof String) {
            return compareTo(new Version((String)o));
        } else if (o instanceof Version) {
            Version other = (Version)o;
            if(variant != null && other.variant == null) return 1;
            if(variant == null && other.variant != null) return -1;
            if(variant != null && other.variant != null) {
                if(!variant.equals(other.variant))
                    return variant.compareTo(other.variant);
            }
            if(major!=other.major) return major - other.major;
            if(minor!=other.minor) return minor - other.minor;
            if(build!=other.build) return build - other.build;
            return revision - other.revision;
        } else {
            throw new IllegalArgumentException("Can't compare Version to " + o.getClass().getCanonicalName());
        }
    }

    public boolean lessThan(Object o) {
        return compareTo(o) < 0;
    }

    public boolean greaterThan(Object o) {
        return compareTo(o) > 0;
    }

    public boolean equalTo(Object o) {
        return compareTo(o) == 0;
    }
}
