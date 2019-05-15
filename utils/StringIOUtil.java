package com.tw.socket.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wei.tian
 * 2019/4/17
 */
public final class StringIOUtil {
    private static final String TAG = "StringIOUtil";
    private final String path;
    private final String name;
    private final boolean isCover;
    private final ExecutorService service;
    private BufferedWriter bw;

    public StringIOUtil(String path, String name, boolean isCover) {
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
            new IllegalStateException("no enough space to save file").printStackTrace();
            return;
        }
        if (!service.isShutdown()) {
            service.execute(() -> {
                try {
                    bw = new BufferedWriter(new FileWriter(file, true));
                } catch (IOException e) {
                    e.printStackTrace();
                    //ignore
                }
            });
        }
    }

    public void write(String string) {
        System.out.println(TAG + " string: " + string);
        if (StringUtil.isBlank(string)) {
            return;
        }
        if (!service.isShutdown()) {
            System.out.println(TAG + " bw: " + bw);
            if (bw != null) {
                service.execute(() -> {
                    try {
                        System.out.println("string: " + string);
                        bw.write(string);
                        bw.flush();
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
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        //ignore
                    } finally {
                        bw = null;
                    }
                }
            });
        }
    }
}
