package com.liuyuan.wifiserver;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.liuyuan.wifiserver.adapter.ChatAdapter;
import com.liuyuan.wifiserver.model.ChatMessage;
import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.p2p.SocketClient;
import com.liuyuan.wifiserver.recorder.Recorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.liuyuan.wifiserver.constant.Global.ORDER_DELETE_RECORD_FILE;
import static com.liuyuan.wifiserver.constant.Global.ORDER_RECEIVE_FILE_FAILED;
import static com.liuyuan.wifiserver.constant.Global.ORDER_RECEIVE_FILE_SUCCEECE;
import static com.liuyuan.wifiserver.constant.Global.ORDER_START_RECORD;
import static com.liuyuan.wifiserver.constant.Global.ORDER_START_SEND_BACK;
import static com.liuyuan.wifiserver.constant.Global.ORDER_START_SEND_FILE_BACK;
import static com.liuyuan.wifiserver.constant.Global.ORDER_STOP_RECORD;
import static com.liuyuan.wifiserver.constant.Global.MSG_SEND_FILE_FAILED;
import static com.liuyuan.wifiserver.constant.Global.MSG_SEND_FILE_SUCCEECE;
import static com.liuyuan.wifiserver.constant.Global.MSG_START_SEND_FILEINFO_BACK;

public class ClientActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ClientActivity.class.getSimpleName();

    private ListView mLvMsgs;

    private ChatAdapter adapter;
    public List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

    private String deviceName;
    private String clientIp;
    private int order;

    private Handler clientHandler;

    private WifiApplication app;
    private Gson gson;
    private Gson gsonFile;

    //录音机
    private Recorder mRecorder;
    //录音文件信息
    private FileInfo mFileInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        initUi();

        deviceName = new Build().MODEL;

        Intent i = getIntent();
        clientIp = i.getStringExtra("clientIp");
        app = (WifiApplication) this.getApplication();
        initClientHandler();
        initClientListener();
    }

    private void initClientListener() {
        if (app.client == null) {
            return;
        }
        Log.d(TAG, "into initClientListener() app client ");
        app.client.setClientMsgListener(new SocketClient.ClientMsgListener() {
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

    private void initClientHandler() {
        //客户端获取传递的ChatMessage
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
                  //客户器端获取命令后完成响应操作
                try {
                    clientOperation(chatMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "into initClientHandler() handleMessage(Message msg) chatMessage =" + chatMsg);
            }
        };
    }

    //客户端响应，录音、发送文件
    private void clientOperation(ChatMessage chatMsg) throws IOException {
        int frequency = chatMsg.getFrequency();
        String format = chatMsg.getFormat();
        order = chatMsg.getOrder();

        switch (order) {
            case ORDER_START_RECORD:
                mRecorder = new Recorder(this, frequency, format);
                mRecorder.startRecord();
                Log.d(TAG, "click startRecording");
                break;
            case ORDER_STOP_RECORD:
                if (mRecorder != null) {
                    mRecorder.stopRecord();
                } else {
                    Toast.makeText(this, "请先录音", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "click stopRecord");
                break;
            case ORDER_DELETE_RECORD_FILE:
                if (mRecorder != null) {
                    mRecorder.deleteFile();
                } else {
                    Toast.makeText(this, "请先录音", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "click deleteFile");
                break;
            case ORDER_START_SEND_BACK:
                if (mRecorder != null) {
                    if (mRecorder.isCompleted()) {
                        //存在录音文件的情况下，client发送文件信息到server
                        mFileInfo = mRecorder.getAudioFileInfo();
                        order = MSG_START_SEND_FILEINFO_BACK;
                        gsonFile = new Gson();
                        String str = gsonFile.toJson(mFileInfo);
                        sendChatMsg(structChatMessage(str));
                    } else {
                        Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show();
                    }
                }
                Log.d(TAG, "click sendRecordFileBack");
                break;
            case ORDER_START_SEND_FILE_BACK:
//                停止发送消息并发送文件
                if (app.client != null){
                    Boolean sendFileComplet =  app.client.sendFile(mFileInfo);
                    if (sendFileComplet){
                        order = MSG_SEND_FILE_SUCCEECE;
                        Log.d(TAG, "MSG_SEND_FILE_SUCCEECE");
                        sendChatMsg(structChatMessage(mFileInfo.getFileName() + " send succeed"));
                    }else {
                        order = MSG_SEND_FILE_FAILED;
                        Log.d(TAG, "MSG_SEND_FILE_FAILED");
                        sendChatMsg(structChatMessage(mFileInfo.getFileName() + " send failed"));
                    }
                }
                break;

            case ORDER_RECEIVE_FILE_SUCCEECE:

                break;
            case ORDER_RECEIVE_FILE_FAILED:

                break;
            default:
                Toast.makeText(this, "无效指令", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initUi() {
        mLvMsgs = (ListView) this.findViewById(R.id.lv_chat);
        adapter = new ChatAdapter(this, chatMessages);
        mLvMsgs.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {

    }

    /**
     * send message
     **/
    private void sendChatMsg(String chatMsg) {
        Log.d(TAG, "into sendChatMsg(ChatMessage msg) msg =" + chatMsg);
        if (app.client != null) {
            app.client.sendMsg(chatMsg);
        }
        Log.d(TAG, "out sendChatMsg(ChatMessage msg) msg =" + chatMsg);
    }

    private String structChatMessage(String text) {
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
//        String strTime = format.format(new Date());

        ChatMessage msg = new ChatMessage();
        msg.setDeviceName(deviceName);
        msg.setNetAddress(clientIp);
        msg.setOrder(order);
        msg.setMsg(text);
//        msg.setMsgTime(strTime);
        chatMessages.add(msg);
        adapter.refreshDeviceList(chatMessages);
        gson = new Gson();
        return gson.toJson(msg);
    }

    @Override
    public void onBackPressed() {
        this.finish();
        super.onBackPressed();
    }
}
