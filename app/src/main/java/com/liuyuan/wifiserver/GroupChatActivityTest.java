
package com.liuyuan.wifiserver;

import android.app.Activity;
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
import android.widget.Toast;

import com.google.gson.Gson;
import com.liuyuan.wifiserver.adapter.ChatAdapter;
import com.liuyuan.wifiserver.model.ChatMessage;
import com.liuyuan.wifiserver.p2p.GameServer.ServerMsgListener;
import com.liuyuan.wifiserver.p2p.SocketClient.ClientMsgListener;
import com.liuyuan.wifiserver.recorder.Recorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupChatActivityTest extends Activity implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = GroupChatActivityTest.class.getSimpleName();

    private ListView mLvMsgs;
    private Spinner spFrequency;
    private Spinner spFormat;

    private ChatAdapter adapter;
    public List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
    
    private String deviceName;
    private String deviceIp;
    
    private Handler serverHandler;
    private Handler clientHandler;
    private WifiApplication app;
    private Gson gson;

    //录音机
    private Recorder mRecorder;
    //录音机频率
    private int mServerFrequency;
    //录音格式
    private String mServerFormat ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actiivty_groupchat);
        initUi();
        
        deviceName = new Build().MODEL;
        deviceIp = "192.168.43.1";
        app = (WifiApplication) this.getApplication();
        initServerHandler();
        initClientHandler();
        
        initServerListener();
        initClientListener();

    }
    
    private void initUi() {
        mLvMsgs = (ListView) this.findViewById(R.id.lv_chat);
        spFrequency = (Spinner) findViewById(R.id.sp_frequency);
        spFormat = (Spinner) findViewById(R.id.sp_format);

        spFrequency.setOnItemSelectedListener(this);
        spFormat.setOnItemSelectedListener(this);

        findViewById(R.id.btn_refresh).setOnClickListener(this);
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
                Log.d(TAG, "into initServerHandler() handleMessage(Message msg)");
                String text = (String) msg.obj;
                sendChatMsg(text);
                gson = new Gson();
                ChatMessage chatMsg = gson.fromJson(text, ChatMessage.class);
                chatMessages.add(chatMsg);
                adapter.refreshDeviceList(chatMessages);
                Log.d(TAG, "into initServerHandler() handleMessage(Message msg) chatMessage = " + chatMsg);
            }
        };
    }

    private void initClientHandler() {
        //服务器端获取传递的ChatMessage
        if (app.client == null) {
            return;
        }
        clientHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "into initClientHandler() handleMessage(Message msg)");
                String text = (String) msg.obj;
                gson = new Gson();
                ChatMessage chatMsg = gson.fromJson(text, ChatMessage.class);
                chatMessages.add(chatMsg);
                adapter.refreshDeviceList(chatMessages);

                //服务器端获取录音参数
                int frequency = chatMsg.getFrequency();
                String format = chatMsg.getFormat();
                Log.d(TAG,"message = "+chatMsg.getMsg() +" frequency = "+frequency + " format = "+format);

                try {
                    clientOperation(chatMsg.getMsg(),frequency,format);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "into initClientHandler() handleMessage(Message msg) chatMessage =" + chatMsg);
            }
        };
    }

    private void clientOperation(String text ,int frequency,String format) throws IOException {
        switch (text){
            case "startPWM":
                mRecorder = new Recorder(this,frequency, format);
                mRecorder.startRecord();
                Log.d(TAG,"click startRecording");
                break;
            case "stopPWM":
                mRecorder.stopRecord();
                Log.d(TAG,"click stopRecord");
                break;
            case "deleteFile":
                mRecorder.deleteFile();
                Log.d(TAG,"click deleteFile");
                break;
            case "sendBack":
                Log.d(TAG,"click sendRecordFileBack");
                break;
            default:
                Toast.makeText(this,"无效指令",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initServerListener() {
        if(app.server == null)  return;
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

    private void initClientListener() {
        if (app.client == null) {
            return;
        }
        Log.d(TAG, "into initClientListener() app client =" + app.client);
        app.client.setClientMsgListener(new ClientMsgListener() {
            
            Message msg = null;
            
            @Override
            public void handlerHotMsg(String hotMsg) {
                Log.d(TAG, "into initClientListener() handlerHotMsg(String hotMsg) hotMsg = " + hotMsg);
                msg = clientHandler.obtainMessage();
                msg.obj = hotMsg;
                clientHandler.sendMessage(msg);
            }
            
            @Override
            public void handlerErorMsg(String errorMsg) {
                // TODO Auto-generated method stub
                
            }
        });
        Log.d(TAG, "out initClientListener()");
    }

    @Override
    public void onClick(View v) {
        String strMsg;
        switch (v.getId()) {
            case R.id.btn_starPWM:
                strMsg ="startPWM";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_stopPWM:
                strMsg = "stopPWM";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_delet:
                strMsg = "deleteFile";
                sendChatMsg(structChatMessage(strMsg));
                break;
            case R.id.btn_sendback:
                strMsg = "sendBack";
                sendChatMsg(structChatMessage(strMsg));
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
        } else if (app.client != null) {
            app.client.sendMsg(chatMsg);
        }
        Log.d(TAG, "out sendChatMsg(ChatMessage msg) msg =" + chatMsg);
    }

    private String structChatMessage(String text) {
        ChatMessage msg = new ChatMessage();
        msg.setDeviceName(deviceName);
        msg.setNetAddress(deviceIp);
        msg.setMsg(text);
        msg.setFrequency(mServerFrequency);
        msg.setFormat(mServerFormat);
        gson = new Gson();
        return gson.toJson(msg);
    }

    @Override
    public void onBackPressed() {
        this.finish();
        super.onBackPressed();
    }

}
