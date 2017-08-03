package com.liuyuan.wifiserver.recorder;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.liuyuan.wifiserver.model.FileInfo;
import com.liuyuan.wifiserver.utils.FileUtils;

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
    private FileInfo audioFile;

    private MediaRecorder mMediaRecorder;

    private boolean sdCardExit;
    private boolean isRecording = false;
    private boolean isComplete;

    private int mFrequency ;
    private String mFormat;

    private Context mContext;



    public Recorder(Context context, int frequency, String format) {
        mFrequency = frequency;
        mFormat = format;
        mContext = context;
        Log.d(TAG,"frequency:"+mFrequency+"formate:"+mFormat);
        initRecorder();
    }

    public void initRecorder() {
        //      判断SD Card是否插入
        sdCardExit = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
//      取得SD Card路径作为录音的文件位置
        if (sdCardExit)
        {
            //创建录音频文件
            String path = FileUtils.ROOT_PATH;
            String fileName = mFrequency+"R"+new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(System.currentTimeMillis()) + ".amr";

            //创建存储录音的文件
            File dirFile = new File(path);
            if(!dirFile.exists()) {
                dirFile.mkdirs();
            }
            mRecAudioFile = new File(dirFile,fileName);

            audioFile = new FileInfo();
            audioFile.setFilePath(FileUtils.ROOT_PATH);
            audioFile.setFileName(fileName);
            audioFile.setFileType(mFormat);

            Log.d(TAG,"创建存储录音文件夹,目标位置"+ mRecAudioFile.getAbsolutePath());
        }else {
            Toast.makeText(mContext,"无SD卡", Toast.LENGTH_SHORT).show();
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

        mMediaRecorder.prepare();
        mMediaRecorder.start();
        Toast.makeText(mContext,"正在录音...",Toast.LENGTH_SHORT).show();
        isRecording = true;
        Log.d(TAG,"startRecord() frequency="+ mFrequency);
    }

    public void stopRecord(){
        if( isRecording){
            //如果正在录音
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false;
            try {
                audioFile.setSize(FileUtils.getFileSizes(mRecAudioFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(mContext,"录音结束,存储录音",Toast.LENGTH_SHORT).show();

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
            audioFile = null;
            Toast.makeText(mContext,"录音结束并删除录音",Toast.LENGTH_SHORT).show();
        }else if(mRecAudioFile != null && mRecAudioFile.exists() && !isRecording){
            //录音已完成
            mRecAudioFile.delete();
            audioFile = null;
            Toast.makeText(mContext,"删除录音",Toast.LENGTH_SHORT).show();
        }else {
            //还未开始录音
            Toast.makeText(mContext,"请先录音",Toast.LENGTH_SHORT).show();
        }
    }

    public Boolean isCompleted() {
        if (mRecAudioFile.exists() && !isRecording)
            isComplete = true;
        else
            isComplete = false;
        return isComplete;
    }
    public FileInfo getAudioFileInfo() {
        return audioFile;
    }


}
