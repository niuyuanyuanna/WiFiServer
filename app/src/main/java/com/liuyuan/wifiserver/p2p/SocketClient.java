package com.liuyuan.wifiserver.p2p;

import android.util.Log;

import com.liuyuan.wifiserver.ClientMainActivity;
import com.liuyuan.wifiserver.WifiApplication;
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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

    private ClientMsgListener mClientMsgListener;
    //flag if got to listen
    private boolean onGoinglistner = true;

    private boolean onSendingMsg = true;

    private FileSender mFileSender;

    /**
     * 发送文件线程列表数据
     */
    private List<FileSender> mFileSenderList = new ArrayList<>();

    private Boolean sendFileSucceed;

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
                        if (!client.isOutputShutdown() && onSendingMsg) {
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
    public Boolean sendFile(final FileInfo fileInfo) {
        Log.i(TAG, "into sendFile()  file:" + fileInfo.toString());
        if (client != null && client.isConnected()) {
            Log.i(TAG, "client != null" + client);
            onSendingMsg = false;

                mFileSender = new FileSender(mContext,client, fileInfo);
                mFileSender.setOnSendListener(new FileSender.OnSendListener() {
                    @Override
                    public void onStart() {
                        Log.d(TAG,"on Start.......................................");
                    }

                    @Override
                    public void onProgress(long progress, long total) {

                    }

                    @Override
                    public void onSuccess(FileInfo fileInfo) {
                        sendFileSucceed = true;
                        Log.d(TAG,"send file" + fileInfo.getFileName() + "succeed !!!!!!!");

                    }

                    @Override
                    public void onFailure(Throwable throwable, FileInfo fileInfo) {
                        sendFileSucceed = false;
                        Log.d(TAG,"send file" + fileInfo.getFileName() + "failed !!!!!!!" +throwable);
                    }
                });
                //添加到线程池执行
                mFileSenderList.add(mFileSender);
                WifiApplication.FILE_SENDER_EXECUTOR.execute(mFileSender);
        }
        return sendFileSucceed;
    }

    public void setClientMsgListener(ClientMsgListener mClientMsgListener) {
        this.mClientMsgListener = mClientMsgListener;
    }

    public static interface ClientMsgListener {
        public void handlerErorMsg(String errorMsg);
        public void handlerHotMsg(String hotMsg);
    }

    public FileSender getmFileSender() {
        return mFileSender;
    }

    public void closeConnection() {
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

    public void restartSendingMessage(){
        onSendingMsg = true;
    }
}
