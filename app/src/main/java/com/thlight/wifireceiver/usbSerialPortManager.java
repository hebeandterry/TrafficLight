package com.thlight.wifireceiver;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class usbSerialPortManager {
	
	private final String ACTION_USB_PERMISSION = "com.thlight.traffic_light.USB_PERMISSION";
	
	PendingIntent mPermissionIntent = null;
	
	UsbManager mUsbManager = null;
	//UsbDevice mUsbDevice   = null;
	UsbDevice device	   		= null;
	
	String OutputStirng = "";

    final int ReceiverScanPeriod = 1000;        //1s
    final int ReceiverStopPeriod = 100;         // 0.1s,  Receiver will scan for ReceiverScanPeriod, and stop ReceiverStopPeriod
	
	private UsbDeviceConnection[] mUsbConnections = null;
	
	private UsbInterface[] mUsbIntfs 	= null;
	private UsbEndpoint[] Endpoint_outs 	= null;
	private UsbEndpoint[] Endpoint_ins  	= null;
	
	private int deviceSize = 0;

	private Context context = null;
	
	boolean isOurDevice = false;
	
	private String DeviceInfoString = "";
	
	private Handler mHandler = null;
	
	static boolean[] aIsReceiver = null;

	public usbSerialPortManager(Context context)
	{
		this.context = context;
		mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
	    
		//mUsbDevice = (UsbDevice) ((Activity) context).getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
		
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbPermissionActionReceiver, filter);      
        
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
	}
	
	/** ================================================ */
	public void setHandler(Handler handler)
	{
		mHandler = handler;
	}
	/** ================================================ */
	public int getSerialPortData(byte[] buf,int index)
	{
		// 1000 : the length of the data to send or receive
		// 500 : timeout	 : in milliseconds
		return mUsbConnections[index].bulkTransfer(Endpoint_ins[index], buf ,1000, 500);
	}

	/** ================================================ */
    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this) 
                {
                   // UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) 
                    {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                    	Log.d("usbManager", "EXTRA_PERMISSION_GRANTED"+device.getDeviceName());
                    	//mUsbConnection = mUsbManager.openDevice(device);
                    	if(mUsbManager.openDevice(device) == null)
                     	{
                     		//Toast.makeText(this, "UsbConnection is null",Toast.LENGTH_SHORT).show();
                     	}
                     	else
                     	{
                     		if(isOurDevice)
                     		{
                                for(int i = 0; i<getDeviceSize(); i++)
                                {
                                    SerialSettingInitial(i);
                                }
                     		}
                     	}
                    }
                    else
                    {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        Toast.makeText(context, String.valueOf("Permission denied for device" + device), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };
    /** ================================================ */
	public void SerialSettingInitial(int index)
	{
        //Performs a control transaction on endpoint zero for this device.
        // 0x40 PC要下給 USB Device 的 Vendor  Command    USB_TYPE_VENDOR
		mUsbConnections[index].controlTransfer(0x40, 0, 0, 0, null, 0, 25);// reset
 		mUsbConnections[index].controlTransfer(0x40, 0, 1, 0, null, 0, 25);//clear Rx
 		mUsbConnections[index].controlTransfer(0x40, 0, 2, 0, null, 0, 25);// clear Tx
 		mUsbConnections[index].controlTransfer(0x40, 0x02, 0x0000, 0, null, 0, 25);	// flow
														        			// control
											                    			// none
 		mUsbConnections[index].controlTransfer(0x40, 0x03,  0x001A, 0, null, 0, 300);// baudrate// 115200
 		mUsbConnections[index].controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 25);	// data bit
														                    // 8, parity
														                    // none,
														                    // stop bit
														                    // 1, tx off
        //Claims exclusive access to a UsbInterface.
        //true to disconnect kernel driver if necessary
 		mUsbConnections[index].claimInterface(mUsbIntfs[index], true);   
 		
 		startScan(index);
 		
 		String GreenLightHexTime = Integer.toHexString(Integer.valueOf(THLApp.green_light) + 1*256);//綠燈 = 256 + 現在的倒數時間
 		
 		String red_time = Integer.toHexString((Integer.valueOf(THLApp.red_light)));
 		
 		String number = Integer.toHexString((Integer.valueOf(THLApp.number)));

        // 把初始的紅燈狀態設成 reference RSSI (2 bytes).
 		if(red_time.length() == 1)
		{
 			red_time = "0"+red_time;
		}
 		// 紅綠燈編號要補成 4 bytes. (Major 位置)
 		if(number.length() == 3)
 			number = "0"+number;
 		else if(number.length() == 2)
 			number = "00"+number;
 		else if(number.length() == 1)
 			number = "000"+number;

 		// 設定傳送給 Beacon 的指令, 預設綠燈的燈號和秒數為 Minor, 然後都會補足 4 bytes.
 		if(GreenLightHexTime.length() == 4)    				
			OutputStirng = THLApp.SET_INFO_COMMAND + " " +number+" "+ GreenLightHexTime+" "+THLApp.REF_RSSI+"\n";
		else if(GreenLightHexTime.length() == 3)
			OutputStirng = THLApp.SET_INFO_COMMAND + " " +number+" 0"+GreenLightHexTime+" "+THLApp.REF_RSSI+"\n";
		else if(GreenLightHexTime.length() == 2)
			OutputStirng = THLApp.SET_INFO_COMMAND + " " +number+" 00"+GreenLightHexTime+" "+THLApp.REF_RSSI+"\n";
		else if(GreenLightHexTime.length() == 1)
			OutputStirng = THLApp.SET_INFO_COMMAND + " " +number+" 000"+GreenLightHexTime+" "+THLApp.REF_RSSI+"\n";
		
 		mHandler.postDelayed(new Runnable() {
			public void run() {

                //Only set Beacon command to beacon.
                for (int i =0; i < getDeviceSize(); i++)
                {
                    if (!usbSerialPortManager.aIsReceiver[i]) {
                        SendCMD(OutputStirng, i);
                    }
                }
			}
		}, 1500);
 		
		mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_LIGHT, 2000);
	}
	/** ================================================ */
	public void USBInitial()
	{
		/*Returns a HashMap containing all USB devices currently attached.*/
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		
		deviceSize = deviceList.size();   //Get the device size.

        aIsReceiver = new boolean[deviceSize];
		for(int i = 0 ; i < deviceSize ; i++)
		{
            aIsReceiver[i] = true;
		}
        
        Toast.makeText(context, deviceList.size()+", USB device(s) found",Toast.LENGTH_SHORT).show();
        
        mUsbConnections = new UsbDeviceConnection[deviceSize];
    	
    	mUsbIntfs 		= new UsbInterface[deviceSize];
    	Endpoint_outs 	= new UsbEndpoint[deviceSize];
    	Endpoint_ins  	= new UsbEndpoint[deviceSize];
        
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        
        int index = 0;
        final int u32ProductId = 5802;
        final int u32VendorId  = 1105;

        while(deviceIterator.hasNext())
        {
        	device = deviceIterator.next();

        	DeviceInfoString += device.getProductId() + " , " + device.getVendorId()+"\n";
        	if(device.getProductId() == u32ProductId && device.getVendorId() == u32VendorId)
        	{
	        	mUsbIntfs[index] = device.getInterface(1);
	        	Endpoint_ins[index]  = mUsbIntfs[index].getEndpoint(0);
	        	Endpoint_outs[index] = mUsbIntfs[index].getEndpoint(1);
	        	DeviceInfoString += device.getDeviceName()+",\n endcount:"+device.getInterface(0).getEndpointCount()+",endpoint:"+device.getInterface(0).getEndpoint(0).getDirection()+",endcount:"
	            		+device.getInterface(1).getEndpointCount()+",endpoint:"+device.getInterface(1).getEndpoint(0).getDirection()+","+device.getInterface(1).getEndpoint(1).getDirection()+"\n";
	            //tv.setText(MessageString+"\n"+"BT MAC:"+deviceBTMac+"WIFI MAC:"+WifiMac);
	            isOurDevice = true;
	            //break;
	            Log.d("usbManager", device.getDeviceName());

                 if(mUsbManager.hasPermission(device))
                 {
                     // Opens the device so it can be used to send and receive data
                     // return : a UsbDeviceConnection, or null if open failed
                    mUsbConnections[index] = mUsbManager.openDevice(device);

                    if(mUsbConnections[index] == null)
                    {
                        Toast.makeText(context, "UsbConnection is null",Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                            SerialSettingInitial(index);
                    }
                 }
                 else
                 {
                      mUsbManager.requestPermission(device, mPermissionIntent);
                 }
	            

        	}
        	index++;
        }
        index = 0;
        
	}
	/** ================================================ */
	public boolean SendCMD(String s,int index)
	{
		//Log.d("debug", s);
		if(mUsbConnections[index] != null)
     	{
			byte[] bytes = null; 
		    try {
				bytes = s.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            //Send command to USB.
            if (bytes != null)
            {
                mUsbConnections[index].bulkTransfer(Endpoint_outs[index], bytes, bytes.length, 1000);
            }

            return true;
     	}
		else
		{
			Toast.makeText(context, "UsbConnection is null",Toast.LENGTH_SHORT).show();

            return false;
		}
	}
	
	/**********************************************/
	public void unregisterReceiver()
	{
		context.unregisterReceiver(mUsbPermissionActionReceiver);
	}
	/***************** 開始掃瞄 ***********************************************/
    public void startScan(int index)
    {
    	String HexTime = Integer.toHexString(ReceiverScanPeriod);  //milliseconds
		String StopHexTime = Integer.toHexString(ReceiverStopPeriod);
		String OutputStirng = "";

        // Command   start_scan 1 ScanTime StopTime.    1 for scanning iBeacon.
		if(HexTime.length() == 4)    				
			OutputStirng = "start_scan 1 "+HexTime+" ";
		else if(HexTime.length() == 3)
			OutputStirng = "start_scan 1 0"+HexTime+" ";
		else if(HexTime.length() == 2)
			OutputStirng = "start_scan 1 00"+HexTime+" ";
		else if(HexTime.length() == 1)
			OutputStirng = "start_scan 1 000"+HexTime+" ";
		
		if(StopHexTime.length() == 4)    				
			OutputStirng = OutputStirng + StopHexTime + "\n";
		else if(StopHexTime.length() == 3)
			OutputStirng = OutputStirng + "0" + StopHexTime + "\n";
		else if(StopHexTime.length() == 2)
			OutputStirng = OutputStirng + "00" + StopHexTime + "\n";
		else if(StopHexTime.length() == 1)
			OutputStirng = OutputStirng + "000" + StopHexTime + "\n";

        //After send command success, start receiving.
		if(SendCMD(OutputStirng,index))
        {
            mHandler.removeMessages(Constants.MSG_RECOGNIZE_RECEIVER);
            mHandler.sendEmptyMessage(Constants.MSG_RECOGNIZE_RECEIVER);
            mHandler.removeMessages(Constants.MSG_RECEIVE);
            mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
        }
    }
    /***************** 停止掃瞄 ***********************************************/
    public void stopScan(int index)
    {
    	SendCMD("stop_scan\n",index);
    }
    /***************** change beacon info ***********************************************/
    public void changeBeacon(int cmd_number ,int table,int color,int beat,int index)
    {
    	String HexCmdNumber = Integer.toHexString(cmd_number);
    	String HexTable = Integer.toHexString(table);
    	String HexColor = Integer.toHexString(color);
    	String HexBeat = Integer.toHexString(beat);
    	
    	if(HexCmdNumber.length() == 1)
    		HexCmdNumber = "0"+HexCmdNumber;
    	if(HexTable.length() == 1)
    		HexTable = "0"+HexTable;
    	if(HexColor.length() == 1)
    		HexColor = "0"+HexColor;
    	if(HexBeat.length() == 1)
    		HexBeat = "0"+HexBeat;
    		
    	SendCMD("set_beacon_info 00112233-4455-6677-8899-AABBCCDDEEFF "+HexCmdNumber+HexTable+" "+
    			HexColor+HexBeat+" "+"00\n",index);
    }
    /*******************************************************************************/
    int getDeviceSize()
    {
    	return deviceSize;
    }
}
