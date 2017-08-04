package com.liuyuan.wifiserver.p2p;

import android.content.Context;
import android.util.Log;

import com.liuyuan.wifiserver.model.FileInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by AA on 2017/3/24.
 */
public class FileSender  implements Runnable {
    private final static String TAG = FileSender.class.getSimpleName();

    /**
     * 字节数组长度
     */
    public static final int BYTE_SIZE_HEADER = 1024 * 10;
    public static final int BYTE_SIZE_DATA = 1024 * 4;

    /**
     * 传输字节类型
     */
    public static final String UTF_8 = "UTF-8";

    private Context mContext;

    /**
     * 待发送的文件数据
     */
    private FileInfo mFileInfo;

    /**
     * 传送文件的Socket输入输出流
     */
    private Socket mSocket;
    private OutputStream mOutputStream;

    /**
     * 用来控制线程暂停、恢复
     */
    private final Object LOCK = new Object();
    private boolean mIsPause;

    /**
     * 该线程是否执行完毕
     */
    private boolean mIsFinish;

    /**
     * 设置未执行线程的不执行标识
     */
    private boolean mIsStop;

    /**
     * 文件传送监听事件
     */
    private OnSendListener mOnSendListener;


    public FileSender(Context context, Socket socket, FileInfo fileInfo) {
        mContext = context;
        mSocket = socket;
        mFileInfo = fileInfo;
    }

    /**
     * 设置发送监听事件
     * @param onSendListener
     */
    public void setOnSendListener(OnSendListener onSendListener) {
        mOnSendListener = onSendListener;
    }

    @Override
    public void run() {
        if(mIsStop) {
            return;
        }

        //初始化
        try {
            if(mOnSendListener != null) {
                mOnSendListener.onStart();
            }
            init();
        } catch (Exception e) {
            e.printStackTrace();
           Log.d(TAG,"FileSender init() --------------------->>> occur expection");
            if(mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //发送文件实体数据
        try {
            parseBody();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"FileSender parseBody() ------->>> occur expection");
            if(mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //文件传输完毕
        try {
            finishTransfer();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"FileSender finishTransfer() ------->>> occur expection");
            if(mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }
    }

    public void init() throws Exception {
        mSocket.setSoTimeout(30 * 1000);
        OutputStream os = mSocket.getOutputStream();
        mOutputStream = new BufferedOutputStream(os);
    }

    public void parseBody() throws Exception {
        long fileSize = mFileInfo.getSize();
        File file = new File(mFileInfo.getFilePath());
        InputStream fis = new FileInputStream(file);

        int len = 0;
        long total = 0;
        byte[] bytes = new byte[BYTE_SIZE_DATA];

        long sTime = System.currentTimeMillis();
        long eTime = 0;
        while ((len = fis.read(bytes)) != -1) {
            synchronized (LOCK) {
                if(mIsPause) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //写入文件
                mOutputStream.write(bytes, 0, len);
                total += len;

                //每隔200毫秒返回一次进度
                eTime = System.currentTimeMillis();
                if(eTime - sTime > 200) {
                    sTime = eTime;
                    if(mOnSendListener != null) {
                        mOnSendListener.onProgress(total, fileSize);
                    }
                }
            }
        }

        //关闭Socket输入输出流
        mOutputStream.flush();
        mOutputStream.close();
        //文件发送成功
        if(mOnSendListener != null) {
            mOnSendListener.onSuccess(mFileInfo);
        }
        mIsFinish = true;
    }

    public void finishTransfer() throws Exception {
        if(mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {

            }
        }

        if(mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 暂停发送线程
     */
    public void pause() {
        synchronized (LOCK) {
            mIsPause = true;
            LOCK.notifyAll();
        }
    }

    /**
     * 恢复发送线程
     */
    public void resume() {
        synchronized (LOCK) {
            mIsPause = false;
            LOCK.notifyAll();
        }
    }

    /**
     * 设置当前的发送任务不执行
     */
    public void stop() {
        mIsStop = true;
    }

    /**
     * 文件是否在发送中
     * @return
     */
    public boolean isRunning() {
        return !mIsFinish;
    }

    public interface OnSendListener {
        void onStart();
        void onProgress(long progress, long total);
        void onSuccess(FileInfo fileInfo);
        void onFailure(Throwable throwable, FileInfo fileInfo);
    }
}
