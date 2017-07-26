package com.liuyuan.wifiserver;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
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
import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.p2p.GameServer;
import com.liuyuan.wifiserver.p2p.SocketClient;
import com.liuyuan.wifiserver.service.WifiHotManager;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemClickListener, WifiHotManager.WifiBroadCastOperator {
    private  static String TAG = MainActivity.class.getSimpleName();
    private TextView mTvStatus;
    private Button mBtnScanHot;
    private Button mBtnCreateHot;
    private Button mBtngotoChat;
    private ListView mLvWifi;


    private WifiHotManager mWifiHotManager;
    /**
     * wifi hot list
     **/
    private WifiHotAdapter mWifiHotAdapter;
    private List<ScanResult> wifiList;
    private String mSSID;
    /**
     * sockt-server if connect
     **/
    private boolean connected;
    private SocketClient mClient;
    private GameServer mGameServer;
    /**
     * server and client
     **/
    private Handler serverHandler;
    private Handler clientHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
    }

    private void initUi() {
        mBtnScanHot = (Button) this.findViewById(R.id.btn_scan);
        mBtnCreateHot = (Button) this.findViewById(R.id.btn_create);
        mBtngotoChat = (Button) this.findViewById(R.id.btn_gotochat);
        mLvWifi = (ListView) this.findViewById(R.id.listHots);
        mTvStatus = (TextView) this.findViewById(R.id.tv_status);

        mBtnScanHot.setOnClickListener(this);
        mBtnCreateHot.setOnClickListener(this);
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

        //初始化serverHandler
        serverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d(TAG, "initServerHandler() ");
                if (msg.what == 0) {
                    Toast.makeText(MainActivity.this, "服务器创建失败", Toast.LENGTH_SHORT).show();
                } else {
                    String text = (String) msg.obj;
                    if (Global.INT_SERVER_SUCCESS.equals(text)) {
                        Toast.makeText(MainActivity.this, "服务器创建成功，等待连接中...", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "into initServerHandler() handleMessage(Message msg) text = " + text);
                    }

                }
            }
        };

    }

    @Override
    protected void onResume() {
        mWifiHotManager = WifiHotManager.getInstance(this, MainActivity.this);
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                // scan wifi hot
                mWifiHotManager.scanWifiHot();
                mTvStatus.setText(R.string.scaning);
                break;

            case R.id.btn_create:
                // close wifi and create hotpot
                boolean isSucceed = mWifiHotManager.startApWifiHot(Global.HOTPOT_NAME);
                if (isSucceed) {
                    //init game server
                    initGameServer();

                    mTvStatus.setText(R.string.list_title);
                    if (mWifiHotAdapter != null) {
                        mWifiHotAdapter.clearData();
                        mWifiHotAdapter.refreshData(wifiList);
                    }
                    Toast.makeText(MainActivity.this, R.string.creating, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.create_failed, Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btn_gotochat:
                Toast.makeText(MainActivity.this, "goto Chat", Toast.LENGTH_SHORT).show();
                //save client/server/mWifiHotManager
                saveData();
                gotoChatActivity();
                break;
            default:
                break;
        }

    }

    private void gotoChatActivity() {
        Intent intent = new Intent();
        intent.setClass(this, GroupChatActivityTest.class);
        this.startActivity(intent);
    }

    //保存数据
    private void saveData() {
        Log.d(TAG, "into saveData()");
        WifiApplication app = (WifiApplication) this.getApplication();
        app.client = this.mClient;
        app.server = this.mGameServer;
        app.wifiHotM = this.mWifiHotManager;
        Log.d(TAG, "out saveData() app client =" + app.client);
    }

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
        Log.d(TAG, "out 刷新wifi热点列表");
    }

    //初始化游戏服务器
    private void initGameServer() {
        mGameServer = GameServer.newInstance(this, Global.PORT, new GameServer.ServerMsgListener() {
            Message msg = null;
            @Override
            public void handlerErrorMsg(String errorMsg) {
                Log.d(TAG, "handleErrorMsg");
                connected = false;
                msg = clientHandler.obtainMessage();
                msg.obj = errorMsg;
                msg.what = 0;
                serverHandler.sendMessage(msg);
            }

            @Override
            public void handlerHotMsg(String hotMsg) {
                Log.d(TAG, "handleHotMsg");
                connected = true;
                msg = clientHandler.obtainMessage();
                msg.obj = hotMsg;
                msg.what = 1;
                serverHandler.sendMessage(msg);
            }
        });
        // start listen port and accept msg from socket callback ServerMsgListener
        mGameServer.beginListenandAcceptMsg();
    }

    //初始化socket客户端
    private void initSocketClient() {
        mClient = SocketClient.newInstance(this, Global.SITE, Global.PORT, new SocketClient.ClientMsgListener() {
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
                mTvStatus.setText("连接中...");
                Log.d(TAG, "into  onItemClick() SSID= " + result.SSID);
                //connect hotpot
                mWifiHotManager.connectToHotpot(result.SSID, wifiList, Global.PASSWORD);
                Log.d(TAG, "out  onItemClick() SSID= " + result.SSID);
                break;

            default:
                break;
        }

    }

    @Override
    public void disPlayWifiScanResult(List<ScanResult> wifiList) {
        Log.d(TAG, "into 扫描结果回调函数");
        mTvStatus.setText(R.string.list_title);
        //Refresh current wifi list
        this.wifiList = wifiList;
        refreshWifiList(wifiList);
        mWifiHotManager.unRegisterWifiScanBroadCast();
        Log.d(TAG, "out 热点扫描结果 ： = " + wifiList);


    }

    @Override
    public boolean disPlayWifiConnResult(boolean result, WifiInfo wifiInfo) {
        if (result) {
            mTvStatus.setText(R.string.connect_succeed);
            mBtngotoChat.setVisibility(View.VISIBLE);
            //after connect successful init client
            initSocketClient();
        } else {
            mTvStatus.setText(R.string.connect_failed);
        }
        return false;
    }


    @Override
    public void operationByType(WifiHotManager.OperationsType operationsType, String ssid) {
        Log.d(TAG, "into operationByType！type = " + operationsType);
        if (operationsType == WifiHotManager.OperationsType.CONNECT) {
            //reconnect hotpot
            mWifiHotManager.connectToHotpot(ssid, wifiList, Global.PASSWORD);
        } else if (operationsType == WifiHotManager.OperationsType.SCAN) {
            mWifiHotManager.scanWifiHot();
        }
        Log.d(TAG, "out operationByType！");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Log.i(TAG, "into onBackPressed()");
            mWifiHotManager.unRegisterWifiScanBroadCast();
            mWifiHotManager.unRegisterWifiStateBroadCast();
            mWifiHotManager.disableWifiHot();
            this.finish();
            Log.i(TAG, "out onBackPressed()");
            return true;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "into onDestroy() ");
        if(mWifiHotAdapter != null) {
            mWifiHotAdapter.clearData();
            mWifiHotAdapter = null;
        }
        if(mGameServer != null) {
            mGameServer.closeConnection();
            mGameServer.stopListener();
            mGameServer = null;
            mWifiHotManager.disableWifiHot();//close wifi hot
        }
        if(mClient != null) {
            mClient.closeConnection();
            mClient.stopAcceptMessage();
            mClient = null;
            mWifiHotManager.deleteMoreCon(mSSID); //remove connected wifihot from exitingConfigs List
        }
        System.exit(0);
        Log.d(TAG, "out onDestroy() ");
        super.onDestroy();
    }
}
