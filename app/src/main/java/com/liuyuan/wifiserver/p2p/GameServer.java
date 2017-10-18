package com.liuyuan.wifiserver.p2p;

import android.os.Build;
import android.util.Log;

import com.liuyuan.wifiserver.ServerMainActivity;
import com.liuyuan.wifiserver.WifiApplication;
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @version V1.0
 * @Description: TODO(chat server to forward to clients)
 */
public class GameServer {

    public static final String TAG = GameServer.class.getSimpleName();
    private static GameServer instance;
    public ServerMainActivity mContext;

    private static HashMap<String, Socket> socketHashMap = new HashMap<>();

    private ServerSocket mServerSocket;
    private int mPort;
    private BufferedReader in;
    private InputStream mInputStream;

    private ServerMsgListener mServerMsgListener;
    private FileReceiver mFileReceiver;
    /**
     * 用来控制发送文件线程暂停、恢复
     */
    private final Object LOCK = new Object();
    private boolean mIsPause = false;
    //文件大小不超过4M
    private static final int BYTE_SIZE_DATA = 1024 * 4;

    //flag if got to listen
    private boolean onGoinglistner = true;

    /**
     * 接收文件线程列表数据
     */
    public static synchronized GameServer newInstance(ServerMainActivity mContext, int port,
                                                      ServerMsgListener serverMsgListener) {
        if (null == instance) {
            instance = new GameServer(mContext, port, serverMsgListener);
        }
        return instance;
    }

    private GameServer(ServerMainActivity mContext, final int port, ServerMsgListener serverListener) {
        Log.d(TAG, "into SocketServer(final int port, ServerMsgListener serverListener) ...................................");
        this.mContext = mContext;
        this.mPort = port;
        this.mServerMsgListener = serverListener;
        Log.d(TAG, "out SocketServer(final int port, ServerMsgListener serverListener) ...................................");
    }

    /**
     * init server to listen
     **/
    public void beginListenandSaveSocket() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try { // init server
                    mServerSocket = new ServerSocket(mPort);
                    mServerMsgListener.handlerHotMsg(Global.INT_SERVER_SUCCESS);
                    Log.d(TAG, "server  =" + mServerSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                    mServerMsgListener.handlerErrorMsg(Global.INT_SERVER_FAIL);
                }
                //server accept from socket msg
                if (mServerSocket != null) {
                    while (onGoinglistner) {
                        try {
                            Socket socket = mServerSocket.accept();
                            if (socket != null ) {
                                String deviceip = null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    deviceip = ((InetSocketAddress) socket.getRemoteSocketAddress()).getHostString();
                                    if (!socketHashMap.containsKey(deviceip)) {
                                        socketHashMap.put(deviceip, socket);
                                        Log.d(TAG, "deviceip ===================" + deviceip);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * accept from socket msg
     */
    public void serverAcceptClientMsg() throws InterruptedException {
        Log.d(TAG, "into serverAcceptClientMsg" + mServerSocket);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (HashMap.Entry<String, Socket> entry : socketHashMap.entrySet()) {
                    Socket socket = entry.getValue();
                    if (socket != null && socket.isConnected()) {
                        while (onGoinglistner) {
                            try {
                                //接收客户端消息
                                mInputStream = socket.getInputStream();
                                in = new BufferedReader(new InputStreamReader(mInputStream, "UTF-8"));
                                String str = in.readLine();
                                if (str == null || str.equals("")) {
                                    break;
                                }
                                if (onGoinglistner){
                                    mServerMsgListener.handlerHotMsg(str);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            }
        }).start();

    }

    /**
     * accept file
     *
     * @param fileInfo
     */
    public void beginAcceptFile(final FileInfo fileInfo, final String deviceip) throws InterruptedException {

        final Socket acceptSocket = socketHashMap.get(deviceip);
        if (acceptSocket != null && acceptSocket.isConnected()) {
            Log.d(TAG, "beginAcceptFile acceptSocket:" + acceptSocket);
            Thread fileReceiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mInputStream = acceptSocket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "FileReceive init() --------------------->>> occur expection");
                    }

                    try {
                        long fileSize = fileInfo.getSize();
                        OutputStream fos = null;
                        fos = new FileOutputStream(FileUtils.gerateLocalFile(fileInfo.getFilePath()));

                        byte[] bytes = new byte[BYTE_SIZE_DATA];
                        long total = 0;
                        int len = 0;

                        long sTime = System.currentTimeMillis();
                        long eTime = 0;
                        while ((len = mInputStream.read(bytes)) != -1) {
                            synchronized (LOCK) {
                                if (mIsPause) {
                                    try {
                                        LOCK.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                //写入文件
                                fos.write(bytes, 0, len);
                                total = total + len;

                                //每隔200毫秒返回一次进度
                                eTime = System.currentTimeMillis();
                                if (eTime - sTime > 200) {
                                    sTime = eTime;
                                    Log.d(TAG, "progress:" + total + "..........total:" + fileSize);
                                }
                            }
                        }
                        socketHashMap.remove(deviceip);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "FileReceive parsebody() --------------------->>> occur expection");
                    }

                    socketHashMap.remove(deviceip);
                    if (socketHashMap == null) {
                        beginListenandSaveSocket();
                    }

                }
            });
            WifiApplication.MAIN_EXECUTOR.execute(fileReceiveThread);
        }
    }

    /**
     * send msg to all Clients
     **/
    public void sendMsgToAllCLients(final String chatMsg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (HashMap.Entry<String, Socket> entry : socketHashMap.entrySet()) {
                    Log.d(TAG, "sendMsgToAllCLients socket:" + entry.getValue() + "\nkeys:" + entry.getKey());
                    sendMsg(entry.getValue(), chatMsg);
                }
            }
        }).start();
    }

    /**
     * send msg to the accept socket
     */
    public void sendMsgToAcceptClient(final String chatMsg, final String deviceip) {
        //通过HashMap找到要接收的socket
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "into sendMsgToAcceptClient() deviceip:" + deviceip);
                Socket acceptSocket = socketHashMap.get(deviceip);
                if (acceptSocket != null && acceptSocket.isConnected()) {
                    sendMsg(acceptSocket, chatMsg);
                    Log.d(TAG, "sendMsgToAcceptClient acceptSocket ip adreess :" + deviceip + " acceptSocket:" + acceptSocket);
                }
            }
        }).start();
    }


    /**
     * send msg to the socket
     **/
    private void sendMsg(Socket client, String chatMsg) {
        Log.i(TAG, "into sendMsg(final Socket client,final ChatMessage msg) msg = " + chatMsg);
        PrintWriter out = null;
        if (client.isConnected()) {
            if (!client.isOutputShutdown()) {
                try {
                    out = new PrintWriter(client.getOutputStream());
                    out.println(chatMsg);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "sendMsg()" + chatMsg + " fail!");
                }
            }
        }
        Log.i(TAG, "out sendMsg(final Socket client,final ChatMessage msg) msg = " + chatMsg);
    }

    public static interface ServerMsgListener {
        public void handlerErrorMsg(String errorMsg);

        public void handlerHotMsg(String hotMsg);
    }

    public void setServerMsgListener(ServerMsgListener mServerMsgListener) {
        this.mServerMsgListener = mServerMsgListener;
    }


    public void closeConnection() {
        Log.i(TAG, "into closeConnection()...................................");
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "out closeConnection()...................................");
    }

    public void stopListener() {
        onGoinglistner = false;
        Log.d(TAG, "stopListener onGoinglistener = " + onGoinglistner);
    }
}
