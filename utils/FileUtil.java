package com.tw.socket.utils;


import java.io.File;

/**
 * Created by wei.tian
 * 2019/4/17
 */
public final class FileUtil {
    private static final int FREE_SPACE_THRESHOLD = 10 * 1024 * 1024;//10M

    private FileUtil() {
        throw new IllegalStateException("No instance");
    }


    public static File createFile(String path, String name, boolean isCover) {
        StringUtil.checkNotBlank(path, "path is blank");
        StringUtil.checkNotBlank(name, "name is blank");
        File dirFile = new File(path);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        File file = new File(path, name);
        if (file.exists()) {
            if (isCover) {
                boolean delete = file.delete();
                System.out.println("FileUtil delete: " + delete);
            } else {
                boolean rename = file.renameTo(new File(path, "old-" + name));
                System.out.println("FileUtil rename: " + rename);
            }
        }
        if (isEnoughSpaceInSdCard(dirFile)) {
            return file;
        } else {
            return null;
        }
    }

    public static boolean isEnoughSpaceInSdCard(File file) {
        return getFreeSpaceLong(file) > FREE_SPACE_THRESHOLD;
    }

    public static long getFreeSpaceLong(File file) {
        if (ObjectUtil.isNull(file)) {
            return -1L;
        }
        return file.getFreeSpace();
    }
}
