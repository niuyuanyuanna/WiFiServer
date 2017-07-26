package com.liuyuan.wifiserver.p2p;

import android.util.Log;

import com.liuyuan.wifiserver.MainActivity;
import com.liuyuan.wifiserver.constant.Global;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: TODO(chat server to forward to clients)
 * @version V1.0
 */
public class GameServer {

    public static final String TAG = GameServer.class.getSimpleName();
    private static GameServer instance;
    public MainActivity mContext;

    public static List<Socket> socketQueue = new ArrayList<Socket>();
    private ServerSocket mServerSocket;
    private int mPort;
    private BufferedReader in;

    private ServerMsgListener mServerMsgListener;
    
    //flag if got to listen
    private boolean onGoinglistner = true;
    /************************************Game*************************/
    public static int count;
    

    public static synchronized GameServer newInstance(MainActivity mContext, int port,
            ServerMsgListener serverMsgListener) {
        if (null == instance) {
            instance = new GameServer(mContext, port, serverMsgListener);
        }
        return instance;
    }

    private GameServer(MainActivity mContext, final int port, ServerMsgListener serverListener) {
        Log.d(TAG, "into SocketServer(final int port, ServerMsgListener serverListener) ...................................");
        this.mContext = mContext;
        this.mPort = port;
        this.mServerMsgListener = serverListener;
        Log.d(TAG, "out SocketServer(final int port, ServerMsgListener serverListener) ...................................");
    }

    /** init server to listen **/
    public void beginListenandAcceptMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { // init server

                    mServerSocket = new ServerSocket();
                    mServerSocket.setReuseAddress(true);
                    InetSocketAddress address = new InetSocketAddress(mPort);
                    mServerSocket.bind(address);
                    mServerMsgListener.handlerHotMsg(Global.INT_SERVER_SUCCESS);
                    Log.d(TAG, "server  =" + mServerSocket);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //server accept from socket msg
                if(mServerSocket != null) {
                    while(onGoinglistner) {
                        try {
                            Socket socket = mServerSocket.accept();
                            if(socket != null) {
                                if(!socketQueue.contains(socket)) {
                                    socketQueue.add(socket);
                                    count++; //记录连接人数
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
     * @param socket
     */
    private void serverAcceptClientMsg(final Socket socket) {
        new Thread(new Runnable(){
            @Override
            public void run() {
                while(!socket.isClosed()) {
                    try {
                    	//此处可以根据连接的客户端数量count做一些数据分发等操作。
                        //接收客户端消息
                    	in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        String str = in.readLine();
                        if(str == null || str.equals("")) {
                            break;
                        }
                        Log.d(TAG, "client" + socket + "str =" + str);
                    	mServerMsgListener.handlerHotMsg(str);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    
    /**send msg to all Clients**/
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

    
    /**send msg to the socket**/
    public void sendMsg(Socket client, String chatMsg) {
        Log.i(TAG, "into sendMsg(final Socket client,final ChatMessage msg) msg = " + chatMsg);
        PrintWriter out = null;
        if (client.isConnected()) {
            if (!client.isOutputShutdown()) {
                try {
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
