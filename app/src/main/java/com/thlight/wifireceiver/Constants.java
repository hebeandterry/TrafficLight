package com.thlight.wifireceiver;

public class Constants {
	public static final int MSG_RECEIVE = 1001;                  //Get the data from the receiver continually.
	public static final int MSG_UPLOAD = 1002;                  //解析receiver收到的資料 然後把它存成list, 去調音量, 每兩秒執行一次
	public static final int MSG_SHOW_MAC = 1003;
	public static final int MSG_CONNECT_SUCCESS = 1004;
	public static final int MSG_SHOW_TCPIP_DATA = 1005;
	public static final int MSG_SHOW_TIME		= 1006;
	public static final int MSG_DELETE_AW_FILE  = 1008;         //Delete the log files that create by Banana Pi
    public static final int MSG_RECOGNIZE_RECEIVER = 1009;     // Check receiver port and beacon port.
	public static final int test  = 1010;
	
	public static final int TCPIP_SET_UPLOAD_TIME 			= 2000; 
	public static final int TCPIP_SET_SCAN_TIME   			= 2001; 
	public static final int TCPIP_SET_SERVER_TYPE_AND_URL 	= 2002; 
	public static final int TCPIP_SET_ALGORITHM			 	= 2003; 
	public static final int TCPIP_SET_UUID_FILTER 			= 2004; 
	public static final int TCPIP_SET_SSID_AND_KEY 			= 2005; 
	public static final int TCPIP_SET_TIME 					= 2006;
	//public static final int TCPIP_SET_NAME 					= 2006;
	public static final int MSG_SHOW_LIGHT					= 3000;

	public static final int BEACON_DATA_FORMAT_LENGTH1    = 71;
	public static final int BEACON_DATA_FORMAT_LENGTH2    = 72;
    public static final int BLE_DATA_FORMAT_LENGTH          = 84;
    public static final int MAC_LENGTH                          = 17;

}
