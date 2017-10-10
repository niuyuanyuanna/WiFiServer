
package com.liuyuan.wifiserver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.gson.Gson;
import com.liuyuan.wifiserver.adapter.ChatAdapter;
import com.liuyuan.wifiserver.model.ChatMessage;
import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.p2p.GameServer.ServerMsgListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.liuyuan.wifiserver.constant.Global.ORDER_DELETE_RECORD_FILE;
import static com.liuyuan.wifiserver.constant.Global.ORDER_START_RECORD;
import static com.liuyuan.wifiserver.constant.Global.ORDER_START_SEND_BACK;
import static com.liuyuan.wifiserver.constant.Global.ORDER_START_SEND_FILE_BACK;
import static com.liuyuan.wifiserver.constant.Global.ORDER_STOP_RECORD;
import static com.liuyuan.wifiserver.constant.Global.MSG_SEND_FILE_FAILED;
import static com.liuyuan.wifiserver.constant.Global.MSG_SEND_FILE_SUCCEECE;
import static com.liuyuan.wifiserver.constant.Global.MSG_START_SEND_FILEINFO_BACK;

public class ServerActivity extends Activity implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = ServerActivity.class.getSimpleName();

    private ListView mLvMsgs;
    private Spinner spFrequency;
    private Spinner spFormat;
    private Context context;

    private ChatAdapter adapter;
    public List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
    private HashMap<String,FileInfo> fileInfoHashMap = new HashMap<>();
    
    private String deviceName;
    private String serverIp;
    private int order;
    
    private Handler serverHandler;
    private WifiApplication app;
    private Gson gson;

    //录音机频率
    private int mServerFrequency;
    //录音格式
    private String mServerFormat ;
    //录音文件信息
    private FileInfo mFileInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actiivty_groupchat);
        initUi();

        Intent intent = getIntent();
        serverIp = intent.getStringExtra("serverIp");

        deviceName = new Build().MODEL;
        app = (WifiApplication) this.getApplication();
        initServerHandler();
        initServerListener();
    }
    
    private void initUi() {
        context = this;
        mLvMsgs = (ListView) this.findViewById(R.id.lv_chat);
        spFrequency = (Spinner) findViewById(R.id.sp_frequency);
        spFormat = (Spinner) findViewById(R.id.sp_format);

        spFrequency.setOnItemSelectedListener(this);
        spFormat.setOnItemSelectedListener(this);

        findViewById(R.id.btn_starPWM).setOnClickListener(this);
        findViewById(R.id.btn_stopPWM).setOnClickListener(this);
        findViewById(R.id.btn_delet).setOnClickListener(this);
        findViewById(R.id.btn_sendback).setOnClickListener(this);

        adapter = new ChatAdapter(this, chatMessages);
        mLvMsgs.setAdapter(adapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //Server端设置参数
        switch (parent.getId()){
            case R.id.sp_frequency:
                mServerFrequency = Integer.parseInt(parent.getItemAtPosition(position).toString());
                Log.d(TAG,"server set frequency:"+mServerFrequency);
                break;
            case R.id.sp_format:
                mServerFormat = parent.getItemAtPosition(position).toString();
                Log.d(TAG,"server set format:"+mServerFormat);
                break;
            default:
                break;
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mServerFrequency = 41000;
        mServerFormat = "amr";

    }

    private void initServerHandler() {
        if (app.server == null) {
            return;
        }
        serverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.i(TAG, "into initServerHandler() handleMessage(Message msg)");
                String text = (String) msg.obj;
                gson = new Gson();
                ChatMessage chatMsg = gson.fromJson(text, ChatMessage.class);
                chatMessages.add(chatMsg);
                adapter.refreshDeviceList(chatMessages);
                try {
                    serverOperation(chatMsg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "out initServerHandler() handleMessage(Message msg) chatMessage = " + chatMsg);
            }
        };
    }

    private void serverOperation(ChatMessage chatMsg) throws InterruptedException {
       switch (chatMsg.getOrder()){
           case MSG_START_SEND_FILEINFO_BACK:
               String str = chatMsg.getMsg();
               String deviceip = chatMsg.getNetAddress();
               String clientName = chatMsg.getDeviceName();
               Log.d(TAG,"serverOperation : the clientip = "+ deviceip);
               mFileInfo = new Gson().fromJson(str, FileInfo.class);
               if (mFileInfo != null){
                   fileInfoHashMap.put(deviceip,mFileInfo);
                   order = ORDER_START_SEND_FILE_BACK;
                   sendChatMsgToTheClient(structChatMessage(clientName+ "please start send file"),deviceip);
                   app.server.beginAcceptFile(mFileInfo,deviceip);
               }
               break;
           case MSG_SEND_FILE_SUCCEECE:
               break;
           case MSG_SEND_FILE_FAILED:
               break;

           default:
               break;
       }
    }

    private void initServerListener() {
        if(app.server == null){
            return;
        }
        Log.d(TAG, "into initServerListener() app server =" + app.server);
        app.server.setServerMsgListener(new ServerMsgListener() {
            Message msg = null;
            @Override
            public void handlerHotMsg(String hotMsg) {
                Log.d(TAG, "into initServerListener() handlerHotMsg(String hotMsg) hotMsg = " + hotMsg);
                msg = serverHandler.obtainMessage();
                msg.obj = hotMsg;
                serverHandler.sendMessage(msg);
            }

            @Override
            public void handlerErrorMsg(String errorMsg) {
                // TODO Auto-generated method stub
                
            }
        });
        Log.d(TAG, "out initServerListener() ");
    }

    @Override
    public void onClick(View v) {
        String strMsg;
        switch (v.getId()) {
            case R.id.btn_starPWM:
                order = ORDER_START_RECORD;
                strMsg = "start record"+",frequence:"+mServerFrequency+",formate:"+mServerFormat;
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_stopPWM:
                order = ORDER_STOP_RECORD;
                strMsg = "stop record";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_delet:
                order = ORDER_DELETE_RECORD_FILE;
                strMsg = "delete file";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_sendback:
                order = ORDER_START_SEND_BACK;
                strMsg = "send file back";
                sendChatMsg(structChatMessage(strMsg));
                break;

            default:
                break;
        }
    }
    
    /**send message**/
    private void sendChatMsg(String chatMsg) {
        Log.d(TAG, "into sendChatMsg(ChatMessage msg) msg =" + chatMsg);
        if (app.server != null) {
            //send msg to all Clients
            app.server.sendMsgToAllCLients(chatMsg);
        }
        Log.d(TAG, "out sendChatMsg(ChatMessage msg) msg =" + chatMsg);
    }

    /**send message to the client**/
    private void sendChatMsgToTheClient(String chatMsg,String deviceip) {
        Log.d(TAG, "into sendChatMsg(ChatMessage msg) msg =" + chatMsg);
        if (app.server != null) {
            //send msg to all Clients
            app.server.sendMsgToAcceptClient(chatMsg,deviceip);
        }
        Log.d(TAG, "out sendChatMsg(ChatMessage msg) msg =" + chatMsg);
    }

    private String structChatMessage(String text) {
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
//        String strTime = format.format(new Date());

        ChatMessage msg = new ChatMessage();
        msg.setDeviceName(deviceName);
        msg.setNetAddress(serverIp);
        msg.setOrder(order);
        msg.setMsg(text);
        msg.setFrequency(mServerFrequency);
        msg.setFormat(mServerFormat);
        chatMessages.add(msg);
        adapter.refreshDeviceList(chatMessages);
//        msg.setMsgTime(strTime);
        gson = new Gson();
        return gson.toJson(msg);
    }

    @Override
    public void onBackPressed() {
        this.finish();
        super.onBackPressed();
    }

}
