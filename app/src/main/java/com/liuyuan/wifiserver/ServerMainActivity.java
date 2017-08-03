package com.liuyuan.wifiserver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.p2p.GameServer;
import com.liuyuan.wifiserver.service.WifiHotAdmin;

public class ServerMainActivity extends AppCompatActivity implements
        View.OnClickListener {
    private static String TAG = ServerMainActivity.class.getSimpleName();
    private TextView mTvStatus;
    private Button mBtnCreateHot;
    private Button mBtngotoChat;


    private WifiHotAdmin mWifiHotAdmin;

    private String serverIp;

    private GameServer mGameServer;
    private Handler serverHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servermain);
        initUi();
    }

    private void initUi() {
        mBtnCreateHot = (Button) this.findViewById(R.id.btn_create);
        mBtngotoChat = (Button) this.findViewById(R.id.btn_gotochat);
        mTvStatus = (TextView) this.findViewById(R.id.tv_status);

        mBtnCreateHot.setOnClickListener(this);
        mBtngotoChat.setOnClickListener(this);

        setStatus("请创建热点");

        //初始化serverHandler
        serverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d(TAG, "initServerHandler() ");
                if (msg.what == 0) {
                    Toast.makeText(ServerMainActivity.this, "服务器创建失败", Toast.LENGTH_SHORT).show();
                } else {
                    String text = (String) msg.obj;
                    if (Global.INT_SERVER_SUCCESS.equals(text)) {
                        Toast.makeText(ServerMainActivity.this, "服务器创建成功，等待连接中...", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "into initServerHandler() handleMessage(Message msg) text = " + text);
                    }
                }
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        mWifiHotAdmin = WifiHotAdmin.newInstance(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_create:
                // close wifi and create hotpot
                boolean isSucceed = mWifiHotAdmin.startWifiAp(Global.HOTPOT_NAME);
                if (isSucceed) {
                    initGameServer();
                    setStatus("成功开启热点...");
                } else {
                    setStatus("开启热点失败，请重试");
                }
                break;

            case R.id.btn_gotochat:
                Toast.makeText(ServerMainActivity.this, "goto Chat", Toast.LENGTH_SHORT).show();
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
        intent.putExtra("serverIp", serverIp);
        intent.setClass(this, ServerActivity.class);
        this.startActivity(intent);
    }

    //保存数据
    private void saveData() {
        Log.d(TAG, "into saveData()");
        WifiApplication app = (WifiApplication) this.getApplication();
        app.server = this.mGameServer;
        Log.d(TAG, "out saveData() app client =" + app.client);
    }

    //初始化服务器socket
    private void initGameServer() {
        Log.d(TAG, "serverIp:" + serverIp);
        mGameServer = GameServer.newInstance(this, Global.PORT, new GameServer.ServerMsgListener() {
            Message msg = null;

            @Override
            public void handlerErrorMsg(String errorMsg) {
                Log.d(TAG, "handleErrorMsg");
                msg = serverHandler.obtainMessage();
                msg.obj = errorMsg;
                msg.what = 0;
                serverHandler.sendMessage(msg);
            }

            @Override
            public void handlerHotMsg(String hotMsg) {
                Log.d(TAG, "handleHotMsg");
                msg = serverHandler.obtainMessage();
                msg.obj = hotMsg;
                msg.what = 1;
                serverHandler.sendMessage(msg);
            }
        });
        // start listen port and accept msg from socket callback ServerMsgListener
        mGameServer.beginListenandAcceptMsg();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Log.i(TAG, "into onBackPressed()");
            mWifiHotAdmin.closeWifiAp();
            this.finish();
            Log.i(TAG, "out onBackPressed()");
            return true;
        }
        return true;
    }


    private void setStatus(String s) {
        mTvStatus.setText(s);
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "into onDestroy() ");
        if (mGameServer != null) {
            mGameServer.closeConnection();
            mGameServer.stopListener();
            mGameServer = null;
            mWifiHotAdmin.closeWifiAp();//close wifi hot
        }

        System.exit(0);
        Log.d(TAG, "out onDestroy() ");
        super.onDestroy();
    }
}
