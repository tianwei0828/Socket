package com.tw.socket.utils;

/**
 * Created by wei.tian
 * 2019/4/4
 */
public final class StringUtil {
    private StringUtil() {
        throw new IllegalStateException("No instance!");
    }

    public static boolean isBlank(String string) {
        return string == null || string.trim().length() == 0;
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    public static void checkNotBlank(String string) {
        checkNotBlank(string, "string is blank");
    }

    public static void checkNotBlank(String string, String message) {
        if (isBlank(string)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static String requireNotBlank(String string) {
        return requireNotBlank(string, "string is blank");
    }

    public static String requireNotBlank(String string, String message) {
        if (isBlank(string)) {
            throw new IllegalArgumentException(message);
        } else {
            return string;
        }
    }
}
