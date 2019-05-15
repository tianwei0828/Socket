package com.tw.socket.client;

/**
 * Created by wei.tian
 * 2019/4/17
 */
public final class Status {
    public static final int CONNECTED = 1000;
    public static final int UNKNOWN_HOST = 1001;
    public static final int NO_PERMISSION = 1002;
    public static final int CONNECT_TIMEOUT = 1003;
    public static final int CONNECT_FAILED = 1004;
    public static final int WRITE_FAILED = 1006;
    public static final int READ_TIMEOUT = 1007;
    public static final int READ_FAILED = 1008;
    public static final int CLOSED = 1009;

    public static final String getMessage(int status) {
        String message = "未知状态";
        switch (status) {
            case CONNECTED:
                message = "连接成功";
                break;
            case UNKNOWN_HOST:
                message = "IP错误";
                break;
            case NO_PERMISSION:
                message = "没有权限";
                break;
            case CONNECT_TIMEOUT:
                message = "连接超时";
                break;
            case CONNECT_FAILED:
                message = "连接失败";
                break;
            case WRITE_FAILED:
                message = "写数据失败";
                break;
            case READ_TIMEOUT:
                message = "读数据超时";
                break;
            case READ_FAILED:
                message = "读数据失败";
                break;
            case CLOSED:
                message = "关闭成功";
                break;
        }
        return message;
    }
}
