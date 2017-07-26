package com.liuyuan.wifiserver.p2p;

import android.util.Log;

import com.liuyuan.wifiserver.MainActivity;
import com.liuyuan.wifiserver.constant.Global;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class SocketClient {
     
    private static final String TAG = SocketClient.class.getSimpleName();
    private static SocketClient instance;
    public MainActivity mContext;

    private static Socket client;
    private String site;
    private int port;
    private BufferedReader in;
    
    private ClientMsgListener mClientMsgListener;
    //flag if got to listen
    private boolean onGoinglistner = true;
    
    /**********************************game****************************************/

    
    public static synchronized SocketClient newInstance(MainActivity mContext, String site, int port,
            ClientMsgListener clientListener) {
        if (null == instance) {
            instance = new SocketClient(mContext,site, port, clientListener);
        }
        Log.i(TAG, "socketClient =" + instance);
        return instance;
    }
    
    private SocketClient(MainActivity mContext, String site, int port, ClientMsgListener clientListener) {
    	this.mContext = mContext;
        this.site = site;
        this.port = port;
        this.mClientMsgListener = clientListener;
    }

    /**after hot pot created and connected successful , start connect GameServer**/
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

    
    /**accept msg from GameServer**/
    private void acceptGameServerMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(onGoinglistner){
                    if(client != null && client.isConnected()) {
                        if(!client.isInputShutdown()) {
                            try {
                                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                String getSMsg = in.readLine();
                                Log.i(TAG, "into acceptMsg()  SMsg =" + getSMsg);
                                if(getSMsg != null) {
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

    /**send msg to GameServer**/
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
    
    public void setClientMsgListener(ClientMsgListener mClientMsgListener) {
        this.mClientMsgListener = mClientMsgListener;
    }
    
    public static interface ClientMsgListener {
        public void handlerErorMsg(String errorMsg);
        public void handlerHotMsg(String hotMsg);
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

}
