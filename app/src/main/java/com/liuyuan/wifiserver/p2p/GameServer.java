package com.liuyuan.wifiserver.p2p;

import android.util.Log;

import com.liuyuan.wifiserver.ServerMainActivity;
import com.liuyuan.wifiserver.WifiApplication;
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.model.FileInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static HashMap<Socket,Boolean> socketIsReceFileHashMap = new HashMap<>();

    private ServerSocket mServerSocket;
    private int mPort;
    private BufferedReader in;

    private ServerMsgListener mServerMsgListener;
    private FileReceiver mFileReceiver;

    //flag if got to listen
    private boolean onGoinglistner = true;

    /**
     * 接收文件线程列表数据
     */
    private List<FileReceiver> mFileReceiverList = new ArrayList<>();

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
    public void beginListenandAcceptMsg() {

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
                            if (socket != null) {
                                if (!socketHashMap.containsValue(socket)) {
                                    String deviceip = null;
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                        deviceip = ((InetSocketAddress) socket.getRemoteSocketAddress()).getHostString();
                                    }
                                    Log.d(TAG, "device ip ===================" + deviceip);
                                    socketHashMap.put(deviceip, socket);
                                    socketIsReceFileHashMap.put(socket,false);
                                }
                                serverAcceptClientMsg(socket);
                            }
                        } catch (IOException | InterruptedException e) {
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
    private void serverAcceptClientMsg(final Socket socket) throws InterruptedException {
        Log.d(TAG, "into serverAcceptClientMsg" + mServerSocket);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!socket.isClosed() && !socketIsReceFileHashMap.get(socket)) {
                    try {
                        //接收客户端消息
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        char[] buffer = new char[1];
                        in.read(buffer,0,1);
                        if (buffer[0] == 'm'){
                            String str = in.readLine();
                            if (str == null || str.equals("")) {
                                break;
                            }
                            Log.d(TAG, "into handle message in the UI xiancheng " + str);
                            mServerMsgListener.handlerHotMsg(str);
                        }else {
                            //接收文件



                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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
    public void beginAcceptFile(FileInfo fileInfo,String deviceip) throws InterruptedException {

        final Socket acceptSocket = socketHashMap.get(deviceip);
        if (acceptSocket != null && acceptSocket.isConnected()) {
            socketIsReceFileHashMap.put(acceptSocket,true);

            Log.d(TAG, "beginAcceptFile acceptSocket:" + acceptSocket);
            mFileReceiver = new FileReceiver(acceptSocket, fileInfo);
            //加入线程池执行
            mFileReceiverList.add(mFileReceiver);
            mFileReceiver.setOnReceiveListener(new FileReceiver.OnReceiveListener() {
                @Override
                public void onStart() {
                    Log.d(TAG, "on Start.......................................");
                }

                @Override
                public void onProgress(FileInfo fileInfo, long progress, long total) {
                }

                @Override
                public void onSuccess(FileInfo fileInfo) {

                    socketIsReceFileHashMap.put(acceptSocket,false);
                    Log.d(TAG, "receive file" + fileInfo.getFileName() + "succeed !!!!!!!");

                }

                @Override
                public void onFailure(Throwable throwable, FileInfo fileInfo) {
                    socketIsReceFileHashMap.put(acceptSocket,false);
                    Log.d(TAG, "receive file" + fileInfo.getFileName() + "failed !!!!!!!");

                }
            });
            WifiApplication.MAIN_EXECUTOR.execute(mFileReceiver);
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
                    out.print('m');
                    out.println(chatMsg);
                    out.flush();
                 } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "sendMsg()"+chatMsg+" fail!");
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
    }

}
