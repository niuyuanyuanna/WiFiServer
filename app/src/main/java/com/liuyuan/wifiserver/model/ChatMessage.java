
package com.liuyuan.wifiserver.model;

/**
 * 
 * @Description: TODO(chat message model) 
 * @author Snail  (Zhanghf QQ:651555765)
 * @date 2014-5-9 下午6:05:39 
 * @version V1.0
 */
public class ChatMessage {
    private String netAddress;
    private String msg;
    private String deviceName;
    private String msgTime;
    private int frequency;
    private String format;


    public String getNetAddress() {
        return netAddress;
    }

    public void setNetAddress(String netAddress) {
        this.netAddress = netAddress;
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
                + deviceName + ", msgTime=" + msgTime + "]";
    }

}
