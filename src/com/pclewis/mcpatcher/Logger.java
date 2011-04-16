package com.pclewis.mcpatcher;

import java.io.PrintStream;

/**
 * Collection of static methods for writing output to MCPatcher's log window.
 */
public class Logger {
    public static final int LOG_NONE = -1;
    public static final int LOG_MAIN = 0;
    public static final int LOG_JAR = LOG_MAIN;
    public static final int LOG_GUI = LOG_MAIN;
    public static final int LOG_MOD = 1;
    public static final int LOG_CLASS = 2;
    public static final int LOG_METHOD = 3;
    public static final int LOG_FIELD = LOG_METHOD;
    public static final int LOG_CONST = LOG_METHOD;
    public static final int LOG_PATCH = 4;
    public static final int LOG_BYTECODE = 5;
    public static final int LOG_REGEX = 6;
    public static final int LOG_DETAIL = 7;

    private static int logLevel = LOG_PATCH;
    private static PrintStream out = System.out;

    static void setLogLevel(int level) {
        logLevel = level;
    }

    /**
     * Check if a given level of logging is enabled.  Use when the data to log is slow to compute.
     *
     * @param level target log level
     * @return true if messages at that level are displayed
     */
    public static boolean isLogLevel(int level) {
        return level <= logLevel;
    }

    static void setOutput(PrintStream out) {
        Logger.out = out;
    }

    static PrintStream getOutput() {
        return out;
    }

    /**
     * Write a blank line to the log.
     *
     * @param level target log level
     */
    public static void log(int level) {
        if (isLogLevel(level)) {
            out.println();
        }
    }

    /**
     * Write a printf-style message to the log followed by a newline.
     *
     * @param level  target loglevel
     * @param format printf-style format string
     * @param params printf-style parameters
     */
    public static void log(int level, String format, Object... params) {
        if (isLogLevel(level)) {
            StringBuilder indent = new StringBuilder(level + format.length() + 1);
            for (int i = 0; i < level; i++) {
                indent.append(' ');
            }
            indent.append(format);
            indent.append('\n');
            out.printf(indent.toString(), params);
        }
    }

    /**
     * Write an exception to the log.
     *
     * @param e exception
     */
    public static void log(Throwable e) {
        e.printStackTrace(out);
    }
}
