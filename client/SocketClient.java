package com.tw.socket.client;

import com.tw.socket.utils.*;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wei.tian
 * 2019/4/17
 */
public final class SocketClient {
    private static final String TAG = "SocketClient";
    private final String server;
    private final int port;
    private final ReadThread readThread;
    private final ExecutorService workerThread;
    private ByteArrayIOUtil byteArrayIOUtil;
    private StringIOUtil stringIOUtil;
    private boolean isDaemon = false;
    private int retryInterval = 2000;
    private int retryCount = 3;
    private int connectTimeout = 0;
    private int readTimeout = 1000 * 60 * 2;
    private boolean isDebug;
    private String logPath;
    private String logName;
    private boolean isCover;
    private Callback callback;
    private Socket socket;
    private InputStream is;
    private BufferedReader br;
    private OutputStream out;
    private BufferedWriter bw;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Object retryLock = new Object();
    private int currentRetryCount = 0;

    public SocketClient(String server, int port) {
        this.server = StringUtil.requireNotBlank(server, "server is blank");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("illegal port number: " + port);
        }
        this.port = port;
        readThread = new ReadThread();
        readThread.setDaemon(isDaemon);
        workerThread = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread result = new Thread(runnable, "SocketClient");
            result.setDaemon(isDaemon);
            return result;
        });
    }

    public void isDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
    }

    public void retryInterval(int retryInterval) {
        this.retryInterval = TimeUtil.checkDuration("retryInterval", retryInterval, TimeUnit.MILLISECONDS);
    }

    public void retryCount(int retryCount) {
        this.retryCount = TimeUtil.checkDuration("retryCount", retryCount, TimeUnit.MILLISECONDS);
    }

    public void readTimeout(int readTimeout) {
        this.readTimeout = TimeUtil.checkDuration("readTimeout", readTimeout, TimeUnit.MILLISECONDS);
    }

    public void connectTimeout(int connectTimeout) {
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("connectTimeout < 0");
        }
        this.connectTimeout = connectTimeout;
    }

    public void isDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public void logPath(String logPath) {
        this.logPath = StringUtil.requireNotBlank(logPath, "logPath == null");
    }

    public void logName(String logName) {
        this.logName = StringUtil.requireNotBlank(logName, "logName == null");
    }

    public void isCover(boolean isCover) {
        this.isCover = isCover;
    }


    public void callback(Callback callback) {
        this.callback = ObjectUtil.requireNotNull(callback, "callback == null");
    }

    public synchronized void connect() {
        if (!workerThread.isShutdown()) {
            workerThread.execute(() -> {
                if (connected.get()) {
                    new IllegalStateException("already connected").printStackTrace();
                    return;
                }
                try {
                    Log.isDebug(isDebug);
                    Log.i(TAG, "isDebug: " + isDebug);
                    Log.i(TAG, "开始连接: " + isDebug);
                    socket = new Socket();
                    InetSocketAddress address = new InetSocketAddress(server, port);
                    socket.setTcpNoDelay(true);
                    socket.connect(address, connectTimeout);
                    socket.setSoTimeout(readTimeout);
                    is = socket.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    out = socket.getOutputStream();
                    bw = new BufferedWriter(new OutputStreamWriter(out));
                    readThread.start();
                    currentRetryCount = 0;
                    connected.set(true);
                    broadcastStatusChanged(Status.CONNECTED);
                    if (StringUtil.isNotBlank(logPath) && StringUtil.isNotBlank(logName)) {
                        byteArrayIOUtil = new ByteArrayIOUtil(logPath, logName, isCover);
                        stringIOUtil = new StringIOUtil(logPath, logName, isCover);
                        byteArrayIOUtil.start();
                        stringIOUtil.start();
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    broadcastStatusChanged(Status.UNKNOWN_HOST);
                } catch (SecurityException e) {
                    e.printStackTrace();
                    broadcastStatusChanged(Status.NO_PERMISSION);
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    if (currentRetryCount < retryCount) {
                        retry();
                    } else {
                        //reconnect failed cause timeout
                        currentRetryCount = 0;
                        broadcastStatusChanged(Status.CONNECT_TIMEOUT);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (currentRetryCount < retryCount) {
                        retry();
                    } else {
                        //reconnect failed
                        currentRetryCount = 0;
                        broadcastStatusChanged(Status.CONNECT_FAILED);
                    }
                }
            });
        }
    }


    public synchronized void write(byte[] data) {
        if (data == null || data.length <= 0) {
            new IllegalArgumentException("data is empty").printStackTrace();
            return;
        }
        write(data, 0, data.length);
    }

    public synchronized void write(byte[] data, int off, int len) {
        if (data == null || data.length <= 0 || off < 0 || len <= 0) {
            new IllegalArgumentException("data is empty").printStackTrace();
            return;
        }
        if (!workerThread.isShutdown()) {
            workerThread.execute(() -> {
                if (!connected.get()) {
                    new IllegalStateException("not connected").printStackTrace();
                    return;
                }
                try {
                    if (out != null) {
                        out.write(data, off, len);
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    broadcastStatusChanged(Status.WRITE_FAILED);
                }
            });
        }
    }

    public synchronized void write(String string) {
        if (StringUtil.isBlank(string)) {
            return;
        }
        if (!workerThread.isShutdown()) {
            workerThread.execute(() -> {
                if (!connected.get()) {
                    new IllegalStateException("not connected").printStackTrace();
                    return;
                }
                try {
                    if (bw != null) {
                        Log.i(TAG, "write: " + string);
                        bw.write(string);
                        bw.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    broadcastStatusChanged(Status.WRITE_FAILED);
                }
            });
        }
    }

    public synchronized void close() {
        synchronized (retryLock) {
            try {
                retryLock.notifyAll();
            } catch (IllegalMonitorStateException e) {
                //ignore
            }
        }
        if (!workerThread.isShutdown()) {
            workerThread.execute(() -> {
                if (!connected.get()) {
                    new IllegalStateException("not connected").printStackTrace();
                    return;
                }
                if (byteArrayIOUtil != null) {
                    byteArrayIOUtil.close();
                }
                if (stringIOUtil != null) {
                    stringIOUtil.close();
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        br = null;
                    }
                }
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        bw = null;
                    }
                }
                if (socket != null) {
                    try {
                        if (socket.isConnected()) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        //ignore
                    } finally {
                        socket = null;
                        currentRetryCount = 0;
                        connected.set(false);
                        broadcastStatusChanged(Status.CLOSED);
                    }
                }
            });
        }
    }

    private void retry() {
        try {
            synchronized (retryLock) {
                long before = System.currentTimeMillis();
                retryLock.wait(retryInterval);
                long after = System.currentTimeMillis();
                if (after - before < retryInterval) {
                    //close被调用，connect failed
                    currentRetryCount = 0;
                    broadcastStatusChanged(Status.CONNECT_FAILED);
                    return;
                }
            }
            connect();
            currentRetryCount++;
            Log.i(TAG, "retry count: " + currentRetryCount);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            //no need to retry
            ////reconnect failed
            currentRetryCount = 0;
            broadcastStatusChanged(Status.CONNECT_FAILED);
        }
    }

    final class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.i(TAG, "ReadThread reading");
            String callbackTypeName = getCallbackTypeName(callback);
            Log.i(TAG, "ReadThread callbackTypeName: " + callbackTypeName);
            if (byte[].class.getSimpleName().equals(callbackTypeName)) {
                bytesRead();
            } else if (String.class.getSimpleName().equals(callbackTypeName)) {
                stringRead();
            }
        }
    }


    private void bytesRead() {
        int len = 0;
        byte[] buffer = new byte[4 * 1024];
        while (connected.get()) {
            if (is != null) {
                try {
                    Log.i(TAG, "开始读数据的时间: " + System.currentTimeMillis());
                    len = is.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    byte[] data = new byte[len];
                    System.arraycopy(buffer, 0, data, 0, len);
                    Log.i(TAG, "读完一组数据的时间: " + System.currentTimeMillis());
                    broadcastDataChanged(data);
                    if (byteArrayIOUtil != null) {
                        byteArrayIOUtil.write(data);
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    //read timeout
                    broadcastStatusChanged(Status.READ_TIMEOUT);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (connected.get()) {
                        broadcastStatusChanged(Status.READ_FAILED);
                    }
                } catch (NullPointerException e) {
                    //ignore
                }
            }
        }
    }

    private void stringRead() {
        while (connected.get()) {
            if (br != null) {
                try {
                    long readStartTime = System.currentTimeMillis();
                    Log.i(TAG, "开始读数据的时间: " + readStartTime);
                    String line = br.readLine();
                    if (StringUtil.isBlank(line)) {
                        break;
                    }
                    long readFinishTime = System.currentTimeMillis();
                    Log.i(TAG, "读完一组数据的时间: " + readFinishTime);
                    Log.i(TAG, "读完一组数据耗时: " + (readFinishTime - readStartTime));
                    Log.i(TAG, "line: " + line);
                    broadcastDataChanged(line);
                    Log.i(TAG, "将line播发耗时: " + (System.currentTimeMillis() - readFinishTime));
                    if (stringIOUtil != null) {
                        stringIOUtil.write(readFinishTime + ":" + line + "\n");
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    //read timeout
                    broadcastStatusChanged(Status.READ_TIMEOUT);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (connected.get()) {
                        broadcastStatusChanged(Status.READ_FAILED);
                    }
                } catch (NullPointerException e) {
                    //ignore
                }
            }
        }
    }

    private void broadcastStatusChanged(int newStatus) {
        if (callback != null) {
            callback.onStatusChanged(newStatus, Status.getMessage(newStatus));
        }
    }

    private <T> void broadcastDataChanged(T data) {
        if (callback != null) {
            Log.i(TAG, "开始播发数据的时间: " + System.currentTimeMillis());
            callback.onDataChanged(data);
            Log.i(TAG, "播发完数据的时间: " + System.currentTimeMillis());
        }
    }

    private String getCallbackTypeName(Callback callback) {
        if (ObjectUtil.isNull(callback)) {
            return null;
        }
        try {
            String[] strings = ((ParameterizedType) (callback.getClass().getGenericInterfaces()[0])).getActualTypeArguments()[0].toString().split("\\.");
            if (strings != null && strings.length > 0) {
                return strings[strings.length - 1];
            }
        } catch (Exception e) {
            e.printStackTrace();
            //ignore
        }
        return null;
    }

    public interface Callback<T> {
        void onDataChanged(T data);

        void onStatusChanged(int status, String message);
    }
}
