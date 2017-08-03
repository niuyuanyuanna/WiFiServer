package com.liuyuan.wifiserver;


import android.app.Application;

import com.liuyuan.wifiserver.p2p.GameServer;
import com.liuyuan.wifiserver.p2p.SocketClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WifiApplication extends Application {

	/**
	 * 主线程池
	 */
	public static Executor MAIN_EXECUTOR = Executors.newFixedThreadPool(5);

	/**
	 * 文件发送端单线程
	 */
	public static Executor FILE_SENDER_EXECUTOR = Executors.newSingleThreadExecutor();
	
    public GameServer server;

    public SocketClient client;

	@Override
	public void onCreate() {
		super.onCreate();
	}

}
