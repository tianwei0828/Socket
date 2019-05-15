# Socket
socket java module
# How to use it ?
## 一、connect
```java
 socketClient = new SocketClient(this.server, this.port);
        //设置重连次数
        socketClient.retryCount(retryCount);
        //设置重连间隔ms
        socketClient.retryInterval(retryInterval);
        //设置连接超时时间ms
        socketClient.connectTimeout(connectTimeout);
        //设置读数据超时时间ms
        socketClient.readTimeout(readTimeout);
        //是否显示log
        socketClient.isDebug(isDebug);
        //存放log的路径
        socketClient.logPath(logPath);
        //存放log的文件名
        socketClient.logName(logName);
        //log是否同名覆盖
        socketClient.isCover(isCover);
        //设置String Callback监听
        socketClient.callback(new SocketClient.Callback<String>() {
            @Override
            public void onDataChanged(String data) {
                if (StringUtil.isBlank(data)) {
                    return;
                }
                //收到从socket读取的数据
            }

            @Override
            public void onStatusChanged(int status, String message) {
                //socket状态和信息
            }
        });
        //设置byte[] Callback监听
         socketClient.callback(new SocketClient.Callback<byte[]>() {

            @Override
            public void onDataChanged(byte[] data) {
               //收到从socket读取的数据
            }

            @Override
            public void onStatusChanged(int status, String message) {
               //socket状态和信息
            }
        });
        //连接
        socketClient.connect();
```
## 二、写数据
```java
  socketClient.write(data);
  socketClient.write(data,off,len);
  socketClient.write(string);
```
## 三、关闭
```java
  socketClient.close();
```
