package com.pclewis.mcpatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies a regular expression to binary input.
 *
 * @see java.util.regex.Matcher
 */
public class BinaryMatcher {
    private String regex;
    private Pattern pattern = null;
    private Matcher matcher = null;
    private String inputStr = null;

    /**
     * Construct a new matcher for the given regular expression.
     *
     * @param objects BinaryRegex elements that make up the expression to match
     * @see BinaryRegex#build(Object...)
     */
    public BinaryMatcher(Object... objects) {
        regex = BinaryRegex.build(objects);
        if (regex != null) {
            pattern = Pattern.compile(this.regex);
        }
    }

    /**
     * Match the regular expression against a particular binary input.
     *
     * @param input  data to match against
     * @param offset position in the input to start looking for matches
     * @return true if match was found
     */
    public boolean match(byte[] input, int offset) {
        if (pattern == null) {
            return false;
        }
        inputStr = BinaryRegex.binToStr(input);

        if (Logger.isLogLevel(Logger.LOG_REGEX)) {
            Logger.log(Logger.LOG_REGEX, "input  = [%s]", inputStr);
            Logger.log(Logger.LOG_REGEX, "offset = [%d]", offset);
            Logger.log(Logger.LOG_REGEX, "regex  = [%s]", regex);
        }

        matcher = pattern.matcher(inputStr);
        if (matcher.find(BinaryRegex.BYTE_LEN * offset)) {
            handleMatch();
            return true;
        } else {
            Logger.log(Logger.LOG_REGEX, "no match");
            return false;
        }
    }

    /**
     * Match the regular expression against a particular binary input.
     *
     * @param input data to match against
     * @return true if match was found
     */
    public boolean match(byte[] input) {
        return match(input, 0);
    }

    /**
     * Get the part of the input before the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @return byte array
     */
    public byte[] getPrefix() {
        return BinaryRegex.strToBin(inputStr.substring(0, matcher.start()));
    }

    /**
     * Get the starting position of the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @return position in input data
     */
    public int getStart() {
        return matcher.start() / BinaryRegex.BYTE_LEN;
    }

    /**
     * Get the ending position of the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @return position in input data
     */
    public int getEnd() {
        return matcher.end() / BinaryRegex.BYTE_LEN;
    }

    /**
     * Get the length of the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @return length
     */
    public int getMatchLength() {
        return (matcher.end() - matcher.start()) / BinaryRegex.BYTE_LEN;
    }

    /**
     * Get the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @return byte array
     */
    public byte[] getMatch() {
        return BinaryRegex.strToBin(inputStr.substring(matcher.start(), matcher.end()));
    }

    /**
     * Get the part of the input after the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @return byte array
     */
    public byte[] getSuffix() {
        return BinaryRegex.strToBin(inputStr.substring(matcher.end()));
    }

    /**
     * Get the value of a captured subexpression from the last match.
     * NOTE: match() must return true before this method can be called.
     *
     * @param group capture group number
     * @return byte array
     */
    public byte[] getCaptureGroup(int group) {
        return BinaryRegex.strToBin(matcher.group(group));
    }

    private void handleMatch() {
        if (matcher.start() % BinaryRegex.BYTE_LEN != 0) {
            throw new RuntimeException(String.format("match start %d is not divisible by %d", matcher.start(), BinaryRegex.BYTE_LEN));
        }
        if (matcher.end() % BinaryRegex.BYTE_LEN != 0) {
            throw new RuntimeException(String.format("match end %d is not divisible by %d", matcher.end(), BinaryRegex.BYTE_LEN));
        }

        if (Logger.isLogLevel(Logger.LOG_REGEX)) {
            Logger.log(Logger.LOG_REGEX, "found it at %d-%d", matcher.start() / BinaryRegex.BYTE_LEN, matcher.end() / BinaryRegex.BYTE_LEN - 1);
            Logger.log(Logger.LOG_REGEX, " prefix = [%s]", inputStr.substring(0, matcher.start()));
            Logger.log(Logger.LOG_REGEX, " match  = [%s]", inputStr.substring(matcher.start(), matcher.end()));
            Logger.log(Logger.LOG_REGEX, " suffix = [%s]", inputStr.substring(matcher.end()));
        }
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.start(i) % BinaryRegex.BYTE_LEN != 0) {
                throw new RuntimeException(String.format("group %d start %d is not divisible by %d", i, matcher.start(i), BinaryRegex.BYTE_LEN));
            }
            if (matcher.end(i) % BinaryRegex.BYTE_LEN != 0) {
                throw new RuntimeException(String.format("group %d end %d is not divisible by %d", i, matcher.end(i), BinaryRegex.BYTE_LEN));
            }
            Logger.log(Logger.LOG_REGEX, " group #%d (%d-%d) = [%s]", i, matcher.start(i) / BinaryRegex.BYTE_LEN, matcher.end(i) / BinaryRegex.BYTE_LEN, matcher.group(i));
        }
    }
}
