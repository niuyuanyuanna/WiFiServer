package com.liuyuan.wifiserver.constant;
/**
 * 
 * @Description: TODO(Constant)
 * @version V1.0
 */
public class Global {

	public static String PASSWORD = "12345678";
	public static String HOTPOT_NAME = "WIFI-HOT-TEST";

	public static int PORT = 8482;


	public static String INT_SERVER_FAIL = "INTSERVER_FAIL";
    public static String INT_SERVER_SUCCESS = "INTSERVER_SUCCESS";
    public static String INT_CLIENT_FAIL = "INTCLIENT_FAIL";
    public static String INT_CLIENT_SUCCESS = "INTCLIENT_SUCCESS";

	//服务器发送开始录音指令
	public static final int ORDER_START_RECORD = 0x661;
	//服务器发送停止录音指令
	public static final int ORDER_STOP_RECORD = 0x662;
	//服务器发送删除录音指令
	public static final int ORDER_DELETE_RECORD_FILE = 0x663;
	//服务器发送录音文件指令
	public static final int ORDER_START_SEND_BACK = 0x664;
	//客户端开始发送录音文件信息指令
	public static final int MSG_START_SEND_FILEINFO_BACK = 0x665;
	//服务器发送开始发送录音文件指令
	public static final int ORDER_START_SEND_FILE_BACK = 0x666;
	//客户端发送录音文件成功
	public static final int MSG_SEND_FILE_SUCCEECE = 0x667;
	//客户端发送录音文件失败
	public static final int MSG_SEND_FILE_FAILED = 0x668;
	//服务器接收录音文件成功
	public static final int ORDER_RECEIVE_FILE_SUCCEECE = 0x669;
	//服务器接收录音文件失败
	public static final int ORDER_RECEIVE_FILE_FAILED = 0x670;



}
