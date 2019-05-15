package com.tw.socket.utils;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wei.tian
 * 2019/4/17
 */
public final class ByteArrayIOUtil {
    private final String path;
    private final String name;
    private final boolean isCover;
    private final ExecutorService service;
    private BufferedOutputStream bos;

    public ByteArrayIOUtil(String path, String name, boolean isCover) {
        this.path = StringUtil.requireNotBlank(path, "path is blank");
        this.name = StringUtil.requireNotBlank(name, "name is blank");
        this.isCover = isCover;
        service = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread result = new Thread(runnable, "FileUtil");
            result.setDaemon(false);
            return result;
        });
    }

    public void start() {
        File file = FileUtil.createFile(path, name, isCover);
        if (ObjectUtil.isNull(file)) {
            new IllegalStateException("no enough space to save file");
            return;
        }
        if (!service.isShutdown()) {
            service.execute(() -> {
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(file, true));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    //ignore
                }
            });
        }
    }

    public void write(byte[] bytes) {
        if (bytes == null && bytes.length <= 0) {
            return;
        }
        if (!service.isShutdown()) {
            if (bos != null) {
                service.execute(() -> {
                    try {
                        bos.write(bytes);
                        bos.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        //ignore
                    }
                });
            }
        }
    }

    public void close() {
        if (!service.isShutdown()) {
            service.execute(() -> {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        //ignore
                    } finally {
                        bos = null;
                    }
                }
            });
        }
    }
}
