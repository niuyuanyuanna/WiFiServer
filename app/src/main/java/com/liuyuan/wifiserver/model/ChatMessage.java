
package com.liuyuan.wifiserver.model;


import com.google.gson.Gson;

public class ChatMessage {
    private String netAddress;
    private int order;
    private String msg;
    private String deviceName;
    private String msgTime;
    private int frequency;
    private String format;


    public String getNetAddress() {
        return netAddress;
    }

    public int getOrder() {
        return order;
    }

    public String getMsg() {
        return msg;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMsgTime() {
        return msgTime;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getFormat() {
        return format;
    }


    public void setNetAddress(String netAddress) {
        this.netAddress = netAddress;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setMsgTime(String msgTime) {
        this.msgTime = msgTime;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "ChatMessage [netAddress=" + netAddress + ", msg=" + msg + ", deviceName="
                + deviceName + ", msgTime=" + msgTime +"]";
    }

}
