package com.tw.socket.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wei.tian
 * 2019/4/4
 */
public final class ObjectUtil {
    private ObjectUtil() {
        throw new IllegalStateException("No instance!");
    }

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }

    public static void checkNotNull(Object object) {
        checkNotNull(object, "object == null");
    }

    public static void checkNotNull(Object object, String message) {
        if (isNull(object)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> T requireNotNull(T t) {
        return requireNotNull(t, "t == null");
    }


    public static <T> T requireNotNull(T t, String message) {
        if (isNull(t)) {
            throw new IllegalArgumentException(message);
        } else {
            return t;
        }
    }

    /**
     * Returns an immutable copy of {@code list}.
     */
    public static <T> List<T> immutableList(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }
}
