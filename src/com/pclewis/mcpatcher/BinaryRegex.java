package com.pclewis.mcpatcher;

/**
 * Collection of static methods for building regular expressions suitable for matching binary data.
 */
public class BinaryRegex {
    private static final String BYTE_REGEX = " \\p{XDigit}{2}";
    private static final String BYTE_FORMAT = " %02x";
    static final int BYTE_LEN = 3;
    private static final String HEX_DIGIT_S = "0123456789abcdef";
    private static final char[] HEX_DIGIT = HEX_DIGIT_S.toCharArray();

    private BinaryRegex() {
    }

    /**
     * Construct an expression from a variable list of parameters.
     *
     * @param objects list of byte, byte[], int, int[], String
     * @return String regex
     */
    public static String build(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            if (o == null) {
                return null;
            } else if (o instanceof Byte) {
                sb.append(literal((Byte) o));
            } else if (o instanceof byte[]) {
                sb.append(literal((byte[]) o));
            } else if (o instanceof Integer) {
                sb.append(literal((Integer) o));
            } else if (o instanceof int[]) {
                sb.append(literal((int[]) o));
            } else if (o instanceof String) {
                sb.append((String) o);
            }
        }
        return sb.toString();
    }

    /**
     * Match a series of bytes exactly.
     *
     * @param b literal
     * @return String regex
     */
    public static String literal(byte[] b) {
        return b == null ? null : binToStr(b);
    }

    /**
     * Match a single byte exactly.
     *
     * @param b literal
     * @return String regex
     */
    public static String literal(byte b) {
        return literal(new byte[]{b});
    }

    /**
     * Match a series of bytes exactly.
     *
     * @param i literal
     * @return String regex
     */
    public static String literal(int[] i) {
        byte[] b = new byte[i.length];
        for (int j = 0; j < b.length; j++) {
            b[j] = (byte) i[j];
        }
        return literal(b);
    }

    /**
     * Match a single byte exactly.
     *
     * @param i literal
     * @return String regex
     */
    public static String literal(int i) {
        return literal((byte) i);
    }

    /**
     * Match an optional expression: (...)?
     *
     * @param regex subexpression
     * @return String regex
     */
    public static String optional(String regex) {
        return regex == null ? "" : String.format("(?:%s)?", regex);
    }

    /**
     * Matches any single byte: .
     *
     * @return String regex
     */
    public static String any() {
        return BYTE_REGEX;
    }

    /**
     * Matches any string of count bytes: (.){m}
     *
     * @param count number of repetitions
     * @return String regex
     */
    public static String any(int count) {
        return repeat(BYTE_REGEX, count);
    }

    /**
     * Matches any string of min...max bytes: (.){m,n}
     *
     * @param min minimum number of repetitions
     * @param max maximum number of repetitions
     * @return String regex
     */
    public static String any(int min, int max) {
        return repeat(BYTE_REGEX, min, max);
    }

    /**
     * Repeats a subexpression exactly count times: (...){m}}
     *
     * @param regex subexpression
     * @param count number of repetitions
     * @return String regex
     */
    public static String repeat(String regex, int count) {
        if (regex != null) {
            return String.format("(?:%s){%d}", regex, count);
        } else if (count > 0) {
            return null;
        } else {
            return "";
        }
    }

    /**
     * Repeats a subexpression between min and max times: (...){m,n}
     *
     * @param regex subexpression
     * @param min   minimum number of repetitions
     * @param max   maximum number of repetitions
     * @return String regex
     */
    public static String repeat(String regex, int min, int max) {
        if (regex != null) {
            return String.format("(?:%s){%d,%d}", regex, min, max);
        } else if (min > 0) {
            return null;
        } else {
            return "";
        }
    }

    /**
     * Matches a specified set of bytes: [...] or [^...]
     *
     * @param byteList array of bytes to match
     * @param positive if true, match bytes; if false, match complement
     * @return String regex
     */
    public static String subset(byte[] byteList, boolean positive) {
        boolean[] allowed = new boolean[256];
        int i;
        for (i = 0; i < allowed.length; i++) {
            allowed[i] = !positive;
        }
        if (byteList != null) {
            for (i = 0; i < byteList.length; i++) {
                allowed[(int) byteList[i] & 0xff] = positive;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(?:");
        boolean first = true;
        for (i = 0; i < allowed.length; i++) {
            if (allowed[i]) {
                if (first) {
                    first = false;
                } else {
                    sb.append("|");
                }
                sb.append(String.format(BYTE_FORMAT, i));
            }
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * Matches any of a list of subexpressions: (exp1|exp2|...)
     *
     * @param regexes variable number of subexpressions
     * @return String regex
     */
    public static String or(String... regexes) {
        StringBuilder sb = new StringBuilder();
        sb.append("(?:");
        boolean first = true;
        for (String regex : regexes) {
            if (regex == null) {
                continue;
            } else if (first) {
                first = false;
            } else {
                sb.append("|");
            }
            sb.append(regex);
        }
        sb.append(")");
        return first ? null : sb.toString();
    }

    /**
     * Capture part of a match to be used later: (...)
     *
     * @param regex subexpression to capture
     * @return String regex
     */
    public static String capture(String regex) {
        return regex == null ? null : "(" + regex + ")";
    }

    /**
     * Match a previously captured subexpression: \1, \2, etc.
     *
     * @param group number of capture group
     * @return String regex
     */
    public static String backReference(int group) {
        return "\\" + group;
    }

    /**
     * Forward looking assertion: (?=...) or (?!...)
     *
     * @param regex    subexpression
     * @param positive if true, assertion is positive, else it is negative
     * @return String regex
     */
    public static String lookAhead(String regex, boolean positive) {
        if (regex != null) {
            return String.format("(?%s%s)", (positive ? "=" : "!"), regex);
        } else if (positive) {
            return null;
        } else {
            return "";
        }
    }

    /**
     * Backward looking assertion: (?<=...) or (?<!...)
     *
     * @param regex    subexpression
     * @param positive if true, assertion is positive, else it is negative
     * @return String regex
     */
    public static String lookBehind(String regex, boolean positive) {
        if (regex != null) {
            return String.format("(?<%s%s)", (positive ? "=" : "!"), regex);
        } else if (positive) {
            return null;
        } else {
            return "";
        }
    }

    static String binToStr(final byte[] b) {
        final int l = b.length;
        final StringBuilder sb = new StringBuilder(BYTE_LEN * l);
        for (int i = 0; i < l; i++) {
            sb.append(' ');
            sb.append(HEX_DIGIT[(b[i] >> 4) & 0xf]);
            sb.append(HEX_DIGIT[b[i] & 0xf]);
        }
        return sb.toString();
    }

    static byte[] strToBin(final String s) {
        final int l = s.length();
        final byte[] b = new byte[l / BYTE_LEN];
        for (int i = 0, j = 0; j < l; i++, j += BYTE_LEN) {
            b[i] = (byte) ((HEX_DIGIT_S.indexOf(s.charAt(j + 1)) << 4) | HEX_DIGIT_S.indexOf(s.charAt(j + 2)));
        }
        return b;
    }
}
