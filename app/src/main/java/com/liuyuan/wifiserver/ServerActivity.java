
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
import java.util.List;

public class ServerActivity extends Activity implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = ServerActivity.class.getSimpleName();

    //服务器发送开始录音指令
    public static final int MSG_ORDER_START_RECORD = 0x661;
    //服务器发送停止录音指令
    public static final int MSG_ORDER_STOP_RECORD = 0x662;
    //服务器发送删除录音指令
    public static final int MSG_ORDER_DELETE_RECORD_FILE = 0x663;
    //服务器发送录音文件指令
    public static final int MSG_ORDER_START_SEND_BACK = 0x664;
    //客户端开始发送录音文件信息指令
    public static final int MSG_START_SEND_FILEINFO_BACK = 0x665;
    //服务器发送开始发送录音文件指令
    public static final int MSG_ORDER_START_SEND_FILE_BACK = 0x666;
    //客户端发送录音文件成功
    public static final int MSG_SEND_FILE_SUCCEECE = 0x667;
    //客户端发送录音文件失败
    public static final int MSG_SEND_FILE_FAILED = 0x668;
    //服务器接收录音文件成功
    public static final int MSG_ORDER_RECEIVE_FILE_SUCCEECE = 0x669;
    //服务器接收录音文件失败
    public static final int MSG_ORDER_RECEIVE_FILE_FAILED = 0x670;


    private ListView mLvMsgs;
    private Spinner spFrequency;
    private Spinner spFormat;
    private Context context;

    private ChatAdapter adapter;
    public List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
    
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
    //接收到的文件信息列表
    public  List<FileInfo> fileInfoList = new ArrayList<FileInfo>();


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
//                sendChatMsg(text);
                gson = new Gson();
                ChatMessage chatMsg = gson.fromJson(text, ChatMessage.class);
                chatMessages.add(chatMsg);
                adapter.refreshDeviceList(chatMessages);
                serverOperation(chatMsg);
                Log.d(TAG, "out initServerHandler() handleMessage(Message msg) chatMessage = " + chatMsg);
            }
        };
    }

    private void serverOperation(ChatMessage chatMsg) {
       switch (chatMsg.getOrder()){
           case MSG_START_SEND_FILEINFO_BACK:
               String str = chatMsg.getMsg();
               String clientName = chatMsg.getDeviceName();
               mFileInfo = new Gson().fromJson(str, FileInfo.class);
               fileInfoList.add(mFileInfo);
               mFileInfo.setPosition(fileInfoList.indexOf(mFileInfo)+1);
               if (mFileInfo != null){
                   order = MSG_ORDER_START_SEND_FILE_BACK;
                   app.server.stopListener();
                   sendChatMsgToTheClient(structChatMessage(clientName+ "please start send file"));
                   app.server.beginAcceptFile(mFileInfo);
//                   if (isSucceed){
//                       order = MSG_ORDER_RECEIVE_FILE_SUCCEECE;
////                       app.server.restartAcceptMsg();
//                       sendChatMsgToTheClient(structChatMessage("receive " + mFileInfo.getFileName() +"succeed!"));
//                   }else {
//                       order = MSG_ORDER_RECEIVE_FILE_FAILED;
//                       sendChatMsgToTheClient(structChatMessage("receive " + mFileInfo.getFileName() +"failed!"));
//                   }
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
                order = MSG_ORDER_START_RECORD;
                strMsg = "start record"+",frequence:"+mServerFrequency+",formate:"+mServerFormat;
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_stopPWM:
                order = MSG_ORDER_STOP_RECORD;
                strMsg = "stop record";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_delet:
                order = MSG_ORDER_DELETE_RECORD_FILE;
                strMsg = "delete file";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_sendback:
                order = MSG_ORDER_START_SEND_BACK;
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
    private void sendChatMsgToTheClient(String chatMsg) {
        Log.d(TAG, "into sendChatMsg(ChatMessage msg) msg =" + chatMsg);
        if (app.server != null) {
            //send msg to all Clients
            app.server.sendMsgToAcceptSocket(chatMsg);
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
