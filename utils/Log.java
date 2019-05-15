package com.tw.socket.utils;

/**
 * Created by wei.tian
 * 2019/4/18
 */
public final class Log {
    private Log() {
        throw new IllegalStateException("No instance");
    }

    private static boolean isDebug;

    public static void isDebug(boolean debug) {
        isDebug = debug;
    }

    public static void i(String tag, String message) {
        if (isDebug) {
            System.out.println(tag + " " + message);
        }
    }
}
