package com.liuyuan.wifiserver.p2p;

import android.util.Log;

import com.liuyuan.wifiserver.ClientMainActivity;
import com.liuyuan.wifiserver.WifiApplication;
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.utils.FileUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class SocketClient {

    private static final String TAG = SocketClient.class.getSimpleName();
    private static SocketClient instance;
    public ClientMainActivity mContext;

    private static Socket client;
    private String site;
    private int port;

    private BufferedReader in;
    private BufferedOutputStream mBufferedOutputStream;

    private ClientMsgListener mClientMsgListener;
    //flag if got to listen
    private boolean onGoinglistner = true;


    /**
     * 用来控制发送文件线程暂停、恢复
     */
    private final Object LOCK = new Object();
    private boolean mIsPause;
    //文件大小不超过4M
    private static final int BYTE_SIZE_DATA = 1024 * 4;

    /**********************************game****************************************/
    public static synchronized SocketClient newInstance(ClientMainActivity mContext, String site, int port,
                                                        ClientMsgListener clientListener) {
        if (null == instance) {
            instance = new SocketClient(mContext, site, port, clientListener);
        }
        Log.i(TAG, "socketClient =" + instance);
        return instance;
    }

    private SocketClient(ClientMainActivity mContext, String site, int port, ClientMsgListener clientListener) {
        this.mContext = mContext;
        this.site = site;
        this.port = port;
        this.mClientMsgListener = clientListener;
    }

    /**
     * after hot pot created and connected successful , start connect GameServer
     **/
    public void connServerandAcceptMsg() {
        Log.i(TAG, "into connectServer()");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client = new Socket(site, port);
                    Log.i(TAG, "Client is created! site:" + site + " port:" + port);
                    //callback
                    mClientMsgListener.handlerHotMsg(Global.INT_CLIENT_SUCCESS);
                    //accept msg from GameServer
                    acceptGameServerMsg();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    mClientMsgListener.handlerErorMsg(Global.INT_CLIENT_FAIL);
                } catch (IOException e) {
                    e.printStackTrace();
                    mClientMsgListener.handlerErorMsg(Global.INT_CLIENT_FAIL);
                }
            }
        }).start();
        Log.i(TAG, "out connectServer()");
    }

    /**
     * accept msg from GameServer
     **/
    private void acceptGameServerMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (onGoinglistner) {
                    if (client != null && client.isConnected()) {
                        if (!client.isInputShutdown()) {
                            try {
                                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                String getSMsg = in.readLine();
                                Log.i(TAG, "into acceptMsg()  SMsg =" + getSMsg);
                                if (getSMsg != null && !getSMsg.equals("")) {
                                    //callback
                                    mClientMsgListener.handlerHotMsg(getSMsg);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * send msg to GameServer
     **/
    public String sendMsg(final String chatMsg) {
        Log.i(TAG, "into sendMsgsendMsg(final ChatMessage msg)  msg =" + chatMsg);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client != null && client.isConnected()) {
                        if (!client.isOutputShutdown()) {
                            PrintWriter out = new PrintWriter(client.getOutputStream());
                            out.println(chatMsg);
                            // out.println(JsonUtil.obj2Str(msg));
                            Log.i(TAG, "成功发送msg =" + chatMsg);
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "client snedMsg error!");
                }
            }
        }).start();
        return "";
    }

    //send file to GameServer
    public void sendFile(final FileInfo fileInfo) {
        Log.i(TAG, "into sendFile()  file:" + fileInfo.toString());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (client != null && client.isConnected()){
                    try {
                        client.setSoTimeout(30 * 1000);
                        OutputStream os = client.getOutputStream();
                        mBufferedOutputStream = new BufferedOutputStream(os);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG,"FileSender init() --------------------->>> occur expection");
                    }


                    try {
                        long fileSize = fileInfo.getSize();
                        File file = new File(fileInfo.getFilePath());
                        InputStream fis = new FileInputStream(file);
                        int len = 0;
                        long total = 0;
                        byte[] bytes = new byte[BYTE_SIZE_DATA];
                        long sTime = System.currentTimeMillis();
                        long eTime = 0;
                        while ((len = fis.read(bytes)) != -1) {
                            synchronized (LOCK) {
                                if(mIsPause) {
                                    try {
                                        LOCK.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                //写入文件
                                mBufferedOutputStream.write(bytes, 0, len);
                                total += len;

                                //每隔200毫秒返回一次进度
                                eTime = System.currentTimeMillis();
                                if(eTime - sTime > 200) {
                                    sTime = eTime;
                                    Log.d(TAG,"progress:"+total+ "..........total:"+fileSize );
                                }
                            }
                        }
                        mBufferedOutputStream.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG,"FileSender parseBody() ------->>> occur expection");
                    }

                }
            }
        }).start();
    }

    public void setClientMsgListener(ClientMsgListener mClientMsgListener) {
        this.mClientMsgListener = mClientMsgListener;
    }

    public static interface ClientMsgListener {
        public void handlerErorMsg(String errorMsg);

        public void handlerHotMsg(String hotMsg);
    }

    public void closeConnection() {
        if(mBufferedOutputStream != null) {
            try {
                mBufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (client != null && client.isConnected()) {
                client.close();
                client = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopAcceptMessage() {
        onGoinglistner = false;
    }
}
