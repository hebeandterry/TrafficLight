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
	
//	UsbDeviceConnection mUsbConnection_1 = null;
//	
//	UsbInterface mUsbIntf_1 	= null;
//	UsbEndpoint Endpoint_out_1 	= null;
//	UsbEndpoint Endpoint_in_1  	= null;
//	
//	UsbDeviceConnection mUsbConnection_2 = null;
//	
//	UsbInterface mUsbIntf_2 	= null;
//	UsbEndpoint Endpoint_out_2 	= null;
//	UsbEndpoint Endpoint_in_2  	= null;
	
	UsbDeviceConnection[] mUsbConnections = null;
	
	UsbInterface[] mUsbIntfs 	= null;
	UsbEndpoint[] Endpoint_outs 	= null;
	UsbEndpoint[] Endpoint_ins  	= null;
	
	int deviceSize = 0;

	Context context = null;
	
	boolean isOurDevice = false;
	
	String DeviceInfoString = "";
	
	Handler mHandler = null;
	
	boolean[] isReceiver = null;

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
                     			SerialSettingInitial(0);
                     			if(getDeviceSize()>1)
                     				SerialSettingInitial(1);
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
 		mUsbConnections[index].claimInterface(mUsbIntfs[index], true);   
 		
 		startScan(index);
 		
 		String GreenLightHexTime = Integer.toHexString(Integer.valueOf(THLApp.green_light));
 		
 		String red_time = Integer.toHexString((Integer.valueOf(THLApp.red_light)));
 		
 		String number = Integer.toHexString((Integer.valueOf(THLApp.number)));
 		
 		if(red_time.length() == 1)
		{
 			red_time = "0"+red_time;
		}
 		
 		if(number.length() == 3)
 			number = "0"+number;
 		else if(number.length() == 2)
 			number = "00"+number;
 		else if(number.length() == 1)
 			number = "000"+number;

 		
 		if(GreenLightHexTime.length() == 4)    				
			OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" "+ GreenLightHexTime+" "+red_time+"\n";
		else if(GreenLightHexTime.length() == 3)
			OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 0"+GreenLightHexTime+" "+red_time+"\n";
		else if(GreenLightHexTime.length() == 2)
			OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 00"+GreenLightHexTime+" "+red_time+"\n";
		else if(GreenLightHexTime.length() == 1)
			OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 000"+GreenLightHexTime+" "+red_time+"\n";
		
 		mHandler.postDelayed(new Runnable() {
			public void run() {
				SendCMD(OutputStirng,0);
				if(getDeviceSize() >1)
				{
					SendCMD(OutputStirng,1);
				}
			}
		}, 1500);
 		
		mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_LIGHT, 2000);
	}
	/** ================================================ */
	public void USBintial()
	{
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		
		deviceSize = deviceList.size();
		
		isReceiver = new boolean[deviceSize];
		for(int i = 0 ; i < deviceSize ; i++)
		{
			isReceiver[i] = true;
		}
        
        Toast.makeText(context, deviceList.size()+", USB device(s) found",Toast.LENGTH_SHORT).show();
        
        mUsbConnections = new UsbDeviceConnection[deviceList.size()];
    	
    	mUsbIntfs 		= new UsbInterface[deviceList.size()];
    	Endpoint_outs 	= new UsbEndpoint[deviceList.size()];
    	Endpoint_ins  	= new UsbEndpoint[deviceList.size()];
        
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        
        int index = 0;
        
        while(deviceIterator.hasNext())
        {
        	
        	device = deviceIterator.next();
        	DeviceInfoString += device.getProductId() + " , " + device.getVendorId()+"\n";
        	if(device.getProductId() == 5802 && device.getVendorId() == 1105)
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
	            
	            if(device != null)
	            {
	            	 if(mUsbManager.hasPermission(device))
	            	 {
	            		mUsbConnections[index] = mUsbManager.openDevice(device);
	       
	                 	if(mUsbConnections[index] == null)
	                 	{
	                 		Toast.makeText(context, "UsbConnection is null",Toast.LENGTH_SHORT).show();
	                 	}
	                 	else
	                 	{
	                 		if(isOurDevice)
	                 		{
	                 			SerialSettingInitial(index);
	                 		}
	                 	}
	                 }
	            	 else
	                 {
	                      mUsbManager.requestPermission(device, mPermissionIntent);
	                 }
	            
	            }
        	}
        	index++;
        }
        index = 0;
           
        
	}
	/** ================================================ */
	public void SendCMD(String s,int index)
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
			int ree = mUsbConnections[index].bulkTransfer(Endpoint_outs[index], bytes, bytes.length, 1000);
//			for(int i = 0 ; i< bytes.length ;i++)
//			{
//				msg +=(char)bytes[i];
//			}
//			//msg += "\n recieve:"+ree+",buf:"+Util.toHexString(bytes);
//			tv.setText(msg);
			
			mHandler.removeMessages(Constants.MSG_RECEIVE);
			//mHandler.removeMessages(Constants.MSG_UPLOAD);
	    			
    	    mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
    	    //mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, Integer.valueOf(THLApp.upload_time));

     	}
		else
		{
			Toast.makeText(context, "UsbConnection is null",Toast.LENGTH_SHORT).show();
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
    	String HexTime = Integer.toHexString(Integer.valueOf(1000));
		String StopHexTime = Integer.toHexString(Integer.valueOf(100));
		String OutputStirng = "";
		if(HexTime.length() == 4)    				
			OutputStirng = "start_scan 3 "+HexTime+" ";
		else if(HexTime.length() == 3)
			OutputStirng = "start_scan 3 0"+HexTime+" ";
		else if(HexTime.length() == 2)
			OutputStirng = "start_scan 3 00"+HexTime+" ";
		else if(HexTime.length() == 1)
			OutputStirng = "start_scan 3 000"+HexTime+" ";
		
		if(StopHexTime.length() == 4)    				
			OutputStirng = OutputStirng + StopHexTime + "\n";
		else if(StopHexTime.length() == 3)
			OutputStirng = OutputStirng + "0" + StopHexTime + "\n";
		else if(StopHexTime.length() == 2)
			OutputStirng = OutputStirng + "00" + StopHexTime + "\n";
		else if(StopHexTime.length() == 1)
			OutputStirng = OutputStirng + "000" + StopHexTime + "\n";
		
		SendCMD(OutputStirng,index);
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
    public int getDeviceSize()
    {
    	return deviceSize;
    }
}
