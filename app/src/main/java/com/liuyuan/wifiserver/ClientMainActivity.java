package com.liuyuan.wifiserver;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.liuyuan.wifiserver.adapter.WifiHotAdapter;
import com.liuyuan.wifiserver.broadcast.WifiBroadcaseReceiver;
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.p2p.SocketClient;
import com.liuyuan.wifiserver.service.WifiMgr;

import java.util.ArrayList;
import java.util.List;

public class ClientMainActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener {
    private static String TAG = ClientMainActivity.class.getSimpleName();
    private TextView mTvStatus;
    private Button mBtnScanHot;
    private Button mBtngotoChat;
    private ListView mLvWifi;

    private WifiMgr mWifiMgr;
    /**
     * wifi hot list
     **/
    private WifiHotAdapter mWifiHotAdapter;
    private List<ScanResult> wifiList = new ArrayList<>();
    private String mSSID; //当前点击的ssid
    private String serverIp;
    private String clientIp;
    /**
     * sockt-server if connect
     **/
    private boolean connected;
    private SocketClient mClient;
    private Handler clientHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientmain);
        initUi();
    }

    private void initUi() {
        mBtnScanHot = (Button) this.findViewById(R.id.btn_scan);
        mBtngotoChat = (Button) this.findViewById(R.id.btn_gotochat);
        mLvWifi = (ListView) this.findViewById(R.id.listHots);
        mTvStatus = (TextView) this.findViewById(R.id.tv_status);

        mBtnScanHot.setOnClickListener(this);
        mBtngotoChat.setOnClickListener(this);
        mLvWifi.setOnItemClickListener(this);

        //初始化clientHandler
        clientHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d(TAG, "initClientHandler() ");
                if (msg.what == 0) {
                    mTvStatus.setText("请检查当前网络连接");
                } else {
                    String text = (String) msg.obj;
                    Log.d(TAG, "into initClientHandler() handleMessage(Message msg) text =" + text);
                }
            }
        };

    }

    @Override
    protected void onResume() {
        mWifiMgr = new WifiMgr(this);
        super.onResume();
    }

    /**
     * 注册监听WiFi操作的系统广播
     */
    private void registerWifiReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiBroadcaseReceiver, filter);
    }

    /**
     * 反注册WiFi相关的系统广播
     */
    private void unregisterWifiReceiver() {
        if (mWifiBroadcaseReceiver != null) {
            unregisterReceiver(mWifiBroadcaseReceiver);
            mWifiBroadcaseReceiver = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                //开启WiFi，监听WiFi广播
                registerWifiReceiver();
                mWifiMgr = WifiMgr.newInstance(this);
                if (mWifiMgr.isApOn()) {
                    mWifiMgr.closeAp();
                    setStatus("关闭热点");
                }
                if (mWifiMgr.isWifiEnabled()) {
                    setStatus("正在扫描可用WiFi...");
                    mWifiMgr.startScan();
                } else {
                    mWifiMgr.openWifi();
                    setStatus("正在打开wifi");
                }
                break;

            case R.id.btn_gotochat:
                Toast.makeText(ClientMainActivity.this, "goto Chat", Toast.LENGTH_SHORT).show();
                //save client/server/mWifiHotManager
                saveData();
                gotoChatActivity();
                break;
            default:
                break;
        }

    }

    private void setStatus(String s) {
        mTvStatus.setText(s);
    }

    private void gotoChatActivity() {
        Intent intent = new Intent();
        intent.putExtra("clientIp", clientIp);
        intent.setClass(this, ClientActivity.class);
        this.startActivity(intent);
    }

    //保存数据
    private void saveData() {
        Log.d(TAG, "into saveData()");
        WifiApplication app = (WifiApplication) this.getApplication();
        app.client = this.mClient;
        Log.d(TAG, "out saveData() app client =" + app.client);
    }

    /**
     * WiFi广播接收器
     */
    private WifiBroadcaseReceiver mWifiBroadcaseReceiver = new WifiBroadcaseReceiver() {
        @Override
        public void onWifiEnabled() {
            //WiFi已开启，开始扫描可用WiFi
            setStatus("正在扫描可用WiFi...");
            mWifiMgr.startScan();
        }

        @Override
        public void onWifiDisabled() {
            //WiFi已关闭，清除可用WiFi列表
            mSSID = "";
            wifiList.clear();
            mWifiHotAdapter.clearData();
        }

        @Override
        public void onScanResultsAvailable(List<ScanResult> scanResults) {
            //扫描周围可用WiFi成功，设置可用WiFi列表
            setStatus("wifi列表");
            wifiList.clear();
            wifiList.addAll(scanResults);
            refreshWifiList(wifiList);
        }

        @Override
        public void onWifiConnected(String connectedSSID) {
            //判断指定WiFi是否连接成功
            if (connectedSSID.equals(Global.HOTPOT_NAME)) {
                //连接成功
                setStatus("Wifi连接成功");
                initSocketClient();

            } else {
                //连接成功的不是设备WiFi，清除该WiFi，重新扫描周围WiFi
                Log.e(TAG, "连接到错误WiFi，正在断开重连...");
                mWifiMgr.disconnectWifi(connectedSSID);
                mWifiMgr.startScan();
            }
        }

        @Override
        public void onWifiDisconnected() {

        }
    };


    //更新wifi列表
    private void refreshWifiList(List<ScanResult> wifiList) {
        Log.d(TAG, "into 刷新wifi热点列表");
        if (null == mWifiHotAdapter) {
            Log.d(TAG, "into 刷新wifi热点列表 adapter is null！");
            mWifiHotAdapter = new WifiHotAdapter(wifiList, this);
            mLvWifi.setAdapter(mWifiHotAdapter);
        } else {
            Log.d(TAG, "into 刷新wifi热点列表 adapter is not null！");
            mWifiHotAdapter.refreshData(wifiList);
        }

    }

    //初始化客户端socket
    private void initSocketClient() {
        serverIp = mWifiMgr.getIpAddressFromHotspot();
        clientIp = mWifiMgr.getLocalIpAddress();
        Log.d(TAG, "serverIp:" + serverIp+"===================================");
        Log.d(TAG, "clientIp:" + clientIp+"===================================");
        mClient = SocketClient.newInstance(this, serverIp, Global.PORT, new SocketClient.ClientMsgListener() {
            Message msg = null;

            @Override
            public void handlerErorMsg(String errorMsg) {
                connected = false;
                Log.d(TAG, "client 初始化失败！");
                msg = clientHandler.obtainMessage();
                msg.obj = errorMsg;
                msg.what = 0;
                clientHandler.sendMessage(msg);
            }

            @Override
            public void handlerHotMsg(String hotMsg) {
                connected = true;
                Log.d(TAG, "client 初始化成功！");
                msg = clientHandler.obtainMessage();
                msg.obj = hotMsg;
                msg.what = 1;
                clientHandler.sendMessage(msg);
            }
        });
        //start thread to conn server and accept msg from server at while loop
        mClient.connServerandAcceptMsg();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.listHots:
                ScanResult result = wifiList.get(position);
                //refresh current mSSID
                mSSID = result.SSID;
                setStatus("正在连接Wifi...");
                Log.d(TAG, "into  onItemClick() SSID= " + result.SSID);

                if ((result.capabilities != null && !result.capabilities.equals(WifiMgr.NO_PASSWORD)) || (result.capabilities != null && !result.capabilities.equals(WifiMgr.NO_PASSWORD_WPS))) {
                    //如果有密码
                    try {
                        mWifiMgr.connectWifi(mSSID, Global.PASSWORD, wifiList);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    //连接免密码WiFi
                    try {
                        setStatus("正在连接Wifi...");
                        mWifiMgr.connectWifi(mSSID, "", wifiList);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "out  onItemClick() SSID= " + result.SSID);
                break;
            default:
                break;
        }

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Log.i(TAG, "into onBackPressed()");
            unregisterWifiReceiver();
            mWifiMgr.disconnectWifi(Global.HOTPOT_NAME);
            this.finish();
            Log.i(TAG, "out onBackPressed()");
            return true;
        }
        return true;
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "into onDestroy() ");
        unregisterWifiReceiver();
        if (mWifiHotAdapter != null) {
            mWifiHotAdapter.clearData();
            mWifiHotAdapter = null;
        }
        if (mClient != null) {
            mClient.closeConnection();
            mClient.stopAcceptMessage();
            mClient = null;
            mWifiMgr.disconnectWifi(Global.HOTPOT_NAME);//remove connected wifihot from exitingConfigs List
        }
        System.exit(0);
        Log.d(TAG, "out onDestroy() ");
        super.onDestroy();
    }
}
