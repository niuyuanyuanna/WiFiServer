package com.liuyuan.wifiserver.recorder;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by liuyuan on 2017/7/25.
 */

public class Recorder {
    private static final String TAG = Recorder.class.getSimpleName();
    //录音使用参数
    private File mRecAudioFile;
    private File mRecAudioDir;// 得到Sd卡path
    private MediaRecorder mMediaRecorder;
    private boolean sdCardExit;
    private boolean isRecording = false;
    private int mFrequency ;
    private String mFormat;

    private Context mContext;


    public Recorder(Context context, int frequency, String format)  {
        mFrequency = frequency;
        mFormat = format;
        mContext = context;
        Log.d(TAG,"frequency:"+mFrequency+"formate:"+mFormat);
//      判断SD Card是否插入
        sdCardExit = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
//      取得SD Card路径作为录音的文件位置
        if (sdCardExit)
        {
            File sdcardDir = Environment.getExternalStorageDirectory();
            //创建录音频文件
            String path = null;
            String filePath = null;
            try {
                path = sdcardDir.getCanonicalPath()+ File.separator + "Recorder";
                //创建存储录音的文件夹
                mRecAudioDir = new File(path);
                if (!mRecAudioDir.exists()) {
                    mRecAudioDir.mkdirs();// 递归创建自定义目录
                }
                filePath = path + "/Recorde"+new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(System.currentTimeMillis())+".amr";
                mRecAudioFile = new File(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG,"创建存储录音文件夹,目标位置"+mRecAudioFile.getAbsolutePath());
        }else {
            Toast.makeText(context,"无SD卡", Toast.LENGTH_SHORT).show();
            return;
        }

    }



    public void startRecord() throws IOException {
        mMediaRecorder = new MediaRecorder();
        //设置录音来源为麦克风
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置采样率
        mMediaRecorder.setAudioSamplingRate(mFrequency);
        //文件保存位置
        mMediaRecorder.setOutputFile(mRecAudioFile.getAbsolutePath());

//        mRecAudioFile.createNewFile();

        mMediaRecorder.prepare();
        mMediaRecorder.start();
        Toast.makeText(mContext,"正在录音...",Toast.LENGTH_SHORT).show();
        isRecording = true;
        Log.d(TAG,"startRecord() frequency="+mFrequency);

    }
    public void stopRecord(){
        if( isRecording){
            //如果正在录音
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false;
            Toast.makeText(mContext,"录音结束",Toast.LENGTH_SHORT).show();
        }else {
            //如果没有在录音
            Toast.makeText(mContext,"未开始录音",Toast.LENGTH_SHORT).show();
        }

    }

    public void deleteFile(){
        if ( isRecording){
            //如果正在录音
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false;
            mRecAudioFile.delete();
            Toast.makeText(mContext,"录音结束并删除录音",Toast.LENGTH_SHORT).show();
        }else if(mRecAudioFile != null && mRecAudioFile.exists() && !isRecording){
            //录音已完成
            mRecAudioFile.delete();
            Toast.makeText(mContext,"删除录音",Toast.LENGTH_SHORT).show();
        }else {
            //还未开始录音
            Toast.makeText(mContext,"请先录音",Toast.LENGTH_SHORT).show();
        }
    }

}
