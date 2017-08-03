package com.liuyuan.wifiserver.model;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.Serializable;
import java.util.List;


public class FileInfo implements Serializable {

    //文件路径
    private String filePath;

    //文件类型
    private String fileType;

    //文件大小
    private long size;

    //文件名
    private String fileName;

    //文件传送结果
    private int result;

    //传输进度
    private int progress;

    public FileInfo( String filePath) {
        this.filePath = filePath;
    }

    public FileInfo() {}

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public static String toJsonStr(FileInfo fileInfo) {
        return new Gson().toJson(fileInfo);
    }

    public static FileInfo toObject(String jsonStr) {
        return new Gson().fromJson(jsonStr, FileInfo.class);
    }

    @Override
    public String toString() {
        return "FileInfo [" + " fileName=" + fileName + ", size=" + size + ",fileType=" + fileType +']';
    }
}
