package com.liuyuan.wifiserver.p2p;

import android.util.Log;

import com.liuyuan.wifiserver.ServerMainActivity;
import com.liuyuan.wifiserver.WifiApplication;
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.utils.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * @version V1.0
 * @Description: TODO(chat server to forward to clients)
 */
public class GameServer {

    public static final String TAG = GameServer.class.getSimpleName();
    private static GameServer instance;
    public ServerMainActivity mContext;

    public static List<Socket> socketQueue = new ArrayList<Socket>();
    private ServerSocket mServerSocket;
    private int mPort;
    private BufferedReader in;
    private Socket acceptSocket; //当前正在传送文件的客户端

    private ServerMsgListener mServerMsgListener;
    private FileReceiver mFileReceiver;

    //flag if got to listen
    private boolean onGoinglistner = true;

    public static int count;
    public int socketPosition;

    private Boolean onReceivingMsg = true;
    private Boolean isReceiveSucceed;
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
                } catch (SocketException e) {
                    e.printStackTrace();
                    mServerMsgListener.handlerErrorMsg(Global.INT_SERVER_FAIL);
                } catch (IOException e) {
                    e.printStackTrace();
                    mServerMsgListener.handlerErrorMsg(Global.INT_SERVER_FAIL);
                }
                //server accept from socket msg
                if (mServerSocket != null) {
                    while (onGoinglistner && onReceivingMsg) {
                        try {
                            Socket socket = mServerSocket.accept();
                            if (socket != null) {
                                if (!socketQueue.contains(socket)) {
                                    socketQueue.add(socket);
                                    count++; //记录连接人数
                                    socketPosition = socketQueue.size();
                                }else {
                                    socketPosition = socketQueue.indexOf(socket)+1;
                                }
                                Log.d(TAG, "接收客户端消息" + socket);
                                serverAcceptClientMsg(socket);
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
     *
     * @param socket
     */
    private void serverAcceptClientMsg(final Socket socket) {
        Log.d(TAG,"into serverAcceptClientMsg"+mServerSocket);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!socket.isClosed() && onReceivingMsg) {
                    try {
                        //接收客户端消息
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        String str = in.readLine();
                        if(str == null || str.equals("")) {
                            break;
                        }
                        if (str.contains(FileUtils.ROOT_PATH)){
                            socketPosition = socketQueue.indexOf(socket);
                        }
                        Log.d(TAG, "client" + socket + "str =" + str);
                        if (onReceivingMsg){
                            mServerMsgListener.handlerHotMsg(str);
                        }else {
                            break;
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
    public Boolean beginAcceptFile(final FileInfo fileInfo) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (acceptSocket !=null){
                    onReceivingMsg = false;
                    mFileReceiver = new FileReceiver(acceptSocket, fileInfo);
                    //加入线程池执行
                    mFileReceiverList.add(mFileReceiver);
                    mFileReceiver.setOnReceiveListener(new FileReceiver.OnReceiveListener() {
                        @Override
                        public void onStart() {
                            Log.d(TAG,"on Start.......................................");
                        }

                        @Override
                        public void onProgress(FileInfo fileInfo, long progress, long total) {
                        }

                        @Override
                        public void onSuccess(FileInfo fileInfo) {
                            isReceiveSucceed = true;
                            Log.d(TAG,"receive file" + fileInfo.getFileName() + "succeed !!!!!!!");

                        }

                        @Override
                        public void onFailure(Throwable throwable, FileInfo fileInfo) {
                            isReceiveSucceed = false;
                            Log.d(TAG,"receive file" + fileInfo.getFileName() + "failed !!!!!!!");

                        }
                    });
                    WifiApplication.MAIN_EXECUTOR.execute(mFileReceiver);
                }
            }
        }).start();
        return isReceiveSucceed;
    }

    /**
     * send msg to all Clients
     **/
    public void sendMsgToAllCLients(final String chatMsg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < socketQueue.size(); i++) {
                    sendMsg(socketQueue.get(i), chatMsg);
                }
            }
        }).start();
    }

    /**
     * send msg to the socket
     **/
    public void sendMsg(Socket client, String chatMsg) {
        Log.i(TAG, "into sendMsg(final Socket client,final ChatMessage msg) msg = " + chatMsg);
        PrintWriter out = null;
        if (client.isConnected()) {
            if (!client.isOutputShutdown()) {
                try {
                    if (chatMsg.contains("please start send file")){
                        socketPosition = socketQueue.indexOf(client);
                    }
                    out = new PrintWriter(client.getOutputStream());
                    out.println(chatMsg);
                    out.flush();
                    Log.i(TAG, "into sendMsg(final Socket client,final ChatMessage msg) msg = " + chatMsg + " success!");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "into sendMsg(final Socket client,final ChatMessage msg) fail!");
                }
            }
        }
        Log.i(TAG, "out sendMsg(final Socket client,final ChatMessage msg) msg = " + chatMsg);
    }

    /**
     * send msg to the accept socket
     *
     * @param chatMsg
     */
    public void sendMsgToAcceptSocket(String chatMsg) {
        acceptSocket = socketQueue.get(socketPosition);
        sendMsg(acceptSocket, chatMsg);

    }

    public static interface ServerMsgListener {
        public void handlerErrorMsg(String errorMsg);
        public void handlerHotMsg(String hotMsg);
    }

    public void setServerMsgListener(ServerMsgListener mServerMsgListener) {
        this.mServerMsgListener = mServerMsgListener;
    }

    public FileReceiver getmFileReceiver() {
        return mFileReceiver;
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

    public void stopReceiveMsg(){
        onReceivingMsg = false;
    }

    public void restartAcceptMsg (){
        onReceivingMsg = true;
    }


}
