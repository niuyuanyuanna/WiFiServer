package com.liuyuan.wifiserver;


import android.app.Application;

import com.liuyuan.wifiserver.p2p.GameServer;
import com.liuyuan.wifiserver.p2p.SocketClient;
import com.liuyuan.wifiserver.service.WifiHotManager;

public class WifiApplication extends Application {

    public GameServer server;

    public SocketClient client;

    public WifiHotManager wifiHotM;
    
	@Override
	public void onCreate() {
		super.onCreate();
	}

}
