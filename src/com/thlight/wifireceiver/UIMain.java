package com.thlight.wifireceiver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.thlight.traffic_light.R;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

public class UIMain extends Activity implements View.OnClickListener , UncaughtExceptionHandler{
	
	
	ScrollView sc = null;
	TextView tv = null;
	TextView tv_volume = null;

	EditText et_red_light 	= null;
	EditText et_green_light = null;
	EditText et_number = null;
	
	Button btn_save = null;
	Button btn_add_volume = null;
	Button btn_reduce_volume = null;
	Button btn_min = null;
	Button btn_max = null;
	
	TextView tv_red = null;
	TextView tv_green = null;
	
	TextClock dc = null;
	

	String MessageString = "";
	
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	int PlayerCount = 1;
	int count = 0;
	
	boolean isUpdated = true;
	
	ArrayList<BeaconInfo> beaconList = new ArrayList<BeaconInfo>();
	ArrayList<BeaconInfo> tempList = new ArrayList<BeaconInfo>();
	
	usbSerialPortManager usbSerialPortManager = null;
	
	//BluetoothAdapter mBTAdapter		= BluetoothAdapter.getDefaultAdapter();
	WifiManager wifiManager = null;
	
	//String deviceBTMac = "";
	String WifiMac = "";
	
	String ServerType = "";
	String Algorithm = "";
	
	String iScanUploadUrl = "http://iscan.atlasyun.com/tagger/multiPost";

	boolean isRed = false;
	
	int light_time = 0;
	
	float soundVolume = 0;
    float soundRate = 0;
    
    private int sp_Id   = 0;
    
    private MediaPlayer mp = null;
    private SoundPool sp = null;
    private AudioManager audioManager = null;
    
    DecimalFormat df = null;
    
    String str1 = "";
    String str2 = "";
	
	Handler mHandler= new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{				
				case Constants.MSG_RECEIVE:
//					if(usbSerialPortManager.isReceiver[0])
//					{
						new Thread(new Runnable() {
							public void run() {
								byte[] buf = new byte[2000]; 
								int re = usbSerialPortManager.getSerialPortData(buf,0);
						
								if(re != -1)
								{
									for(int i = 0 ; i< re ;i++)
									{
										str1 +=(char)buf[i];
									}								
									//Log.d("debug", "re:"+re+","+new String(str1));
									//tv.setText(MessageString);
									if(isReceiver(str1))
									{
										if(MessageString.length()<500000)
											MessageString = MessageString + new String(str1);
										//Log.d("usbManager", "usb1:true"+","+MessageString.length());
										usbSerialPortManager.isReceiver[0] = true;
									}
									else
									{
		//								MessageString = "";
										usbSerialPortManager.isReceiver[0] = false;
										Log.d("usbManager", "usb1:false");
									}
								}
								str1 = "";
								buf = null;
								
							}
						}).start();
//					}
					
//					if(usbSerialPortManager.isReceiver[1])
//					{
						new Thread(new Runnable() {
							public void run() {
								if(usbSerialPortManager.getDeviceSize() >1)
								{
									byte[] buf_2 = new byte[2000]; 
									int re_2 = usbSerialPortManager.getSerialPortData(buf_2,1);
									
									if(re_2 != -1)
									{
										for(int i = 0 ; i< re_2 ;i++)
										{
											str2 +=(char)buf_2[i];
										}
										
										//Log.d("debug", "re2:"+re_2+","+new String(str2));
										//tv.setText(MessageString);
										if(isReceiver(str2))
										{
											if(MessageString.length()<500000)
												MessageString = MessageString + new String(str2);
											
											usbSerialPortManager.isReceiver[0] = true;
											//Log.d("usbManager", "usb2:true"+","+MessageString.length());
										}
										else
										{
		//									MessageString = "";
											usbSerialPortManager.isReceiver[1] = false;
											Log.d("usbManager", "usb2:false");
										}
									}	
									str2 = "";
									buf_2 = null;
								}
							}
						}).start();
//					}
				
					mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,40);
					break;
				case Constants.MSG_CONNECT_SUCCESS:
					tv.setText("connect success");
					break;			
				case Constants.MSG_SHOW_LIGHT:
					
					light_time = light_time-1;
					
					if(light_time == 0 || light_time < 0)
					{
						sp.setRate(sp_Id, 1.0f);
						isRed = !isRed;
						if(isRed)
						{
							light_time = Integer.valueOf(THLApp.red_light);
							tv_green.setBackgroundResource(R.drawable.gray_light);
							tv_green.setText("");
							tv_red.setBackgroundResource(R.drawable.red_light);
						}
						else
						{
							light_time = Integer.valueOf(THLApp.green_light);
							tv_green.setBackgroundResource(R.drawable.green_light);
							tv_red.setBackgroundResource(R.drawable.gray_light);
							tv_red.setText("");
						}
					}
					
					String OutputStirng = "";
					String LightHexTime = "";
					
					String number = Integer.toHexString((Integer.valueOf(THLApp.number)));
    		 		
    		 		if(number.length() == 3)
    		 			number = "0"+number;
    		 		else if(number.length() == 2)
    		 			number = "00"+number;
    		 		else if(number.length() == 1)
    		 			number = "000"+number;
					
					if(!isRed)
					{
						String red_time = Integer.toHexString((Integer.valueOf(THLApp.red_light)));
						if(red_time.length() == 1)
						{
							red_time = "0"+red_time;
						}
						LightHexTime = Integer.toHexString(Integer.valueOf(light_time) + 1*256);//綠燈 = 256 + 現在的倒數時間
						
	    		 		if(LightHexTime.length() == 4)    				
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" "+LightHexTime+" "+red_time+"\n";
	    				else if(LightHexTime.length() == 3)
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 0"+LightHexTime+" "+red_time+"\n";
	    				else if(LightHexTime.length() == 2)
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 00"+LightHexTime+" "+red_time+"\n";
	    				else if(LightHexTime.length() == 1)
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 000"+LightHexTime+" "+red_time+"\n";
					}
					else {
						String green_time = Integer.toHexString((Integer.valueOf(THLApp.green_light)));
						if(green_time.length() == 1)
						{
							green_time = "0"+green_time;
						}
						LightHexTime = Integer.toHexString(Integer.valueOf(light_time) + 2*256);//紅燈 = 2*256 + 現在的倒數時間
						
	    		 		if(LightHexTime.length() == 4)    				
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" "+LightHexTime+" "+green_time+"\n";
	    				else if(LightHexTime.length() == 3)
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 0"+LightHexTime+" "+green_time+"\n";
	    				else if(LightHexTime.length() == 2)
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 00"+LightHexTime+" "+green_time+"\n";
	    				else if(LightHexTime.length() == 1)
	    					OutputStirng = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1 "+number+" 000"+LightHexTime+" "+green_time+"\n";
					}
					
    		 		if(isRed)
    		 			tv_red.setText(""+light_time);
    		 		else
    		 			tv_green.setText(""+light_time);
    		 		
					tv.setText("time:"+light_time);
					
					if(light_time <= 5 && !isRed)
					{
						mp.start();
					}
					else if(light_time%2 == 0 && !isRed)
					{					
						mp.start();
					}
					//Log.d("debug", "isRed:"+isRed + "time:"+light_time);
					mHandler.removeMessages(Constants.MSG_SHOW_LIGHT);
					mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_LIGHT,1000);
					
					if(!usbSerialPortManager.isReceiver[0])
					{
						usbSerialPortManager.SendCMD(OutputStirng,0);
					}
					if(!usbSerialPortManager.isReceiver[1])
					{
	    				if(usbSerialPortManager.getDeviceSize() >1)
	    				{
	    					usbSerialPortManager.SendCMD(OutputStirng,1);
	    				}
					}
					break;
				case Constants.MSG_UPLOAD:		
					mHandler.removeMessages(Constants.MSG_RECEIVE);
					mHandler.removeMessages(Constants.MSG_UPLOAD);
					parseBeaconDataToList();
					tempList = null;
					tempList = cloneList(beaconList);
					beaconList.clear();
					//Log.d("debug", "beacon size:"+tempList.size());
					for(int i = 0 ; i < tempList.size() ; i++)
					{
						Log.d("beacon", "major:"+tempList.get(i).major + ",minor:"+tempList.get(i).minor);
						THLApp.volume = Float.valueOf(tempList.get(i).major)/100;
						tv_volume.setText(THLApp.volume + "");
						mp.setVolume(THLApp.volume, THLApp.volume);
					}
					mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
					mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD,2000);
					break;
				case Constants.MSG_DELETE_AW_FILE:
					File file = new File(Environment.getExternalStorageDirectory().toString());
					File[] files = file.listFiles();
					for(int i = 0; i<files.length;i++)
					{
						Log.d("debug",files[i].getName());
						if(files[i].getName().startsWith("aw_"))
						{
							files[i].delete();
						}
					}
					mHandler.sendEmptyMessageDelayed(Constants.MSG_DELETE_AW_FILE, 3600000);
					break;
			}
		}
	};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_main);
        
        Thread.setDefaultUncaughtExceptionHandler(this); 
        
        usbSerialPortManager = new usbSerialPortManager(this);
        usbSerialPortManager.setHandler(mHandler);
        
        mp = MediaPlayer.create(this, R.raw.light);
        sp = new SoundPool(1,AudioManager.STREAM_MUSIC,5);
        
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setParameters("audio_devices_out_active=AUDIO_CODEC");
        //audioManager.setParameters("audio_devices_out_active=AUDIO_HDMI");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        
        sp_Id = sp.load(this, R.raw.light,1);
 
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
       
        tv = (TextView)findViewById(R.id.tv_log);
        tv_volume = (TextView)findViewById(R.id.tv_volume);
        
        dc = (TextClock)findViewById(R.id.dc);

        sc = (ScrollView)findViewById(R.id.sv);
        
        btn_save = (Button)findViewById(R.id.btn_save);
        btn_save.setOnClickListener(this);
        
        btn_add_volume = (Button)findViewById(R.id.btn_add_volume);
        btn_add_volume.setOnClickListener(this);
        
        btn_reduce_volume = (Button)findViewById(R.id.btn_reduce_volume);
        btn_reduce_volume.setOnClickListener(this);
        
        btn_min = (Button)findViewById(R.id.btn_min);
        btn_min.setOnClickListener(this);
        
        btn_max = (Button)findViewById(R.id.btn_max);
        btn_max.setOnClickListener(this);
        
        et_red_light = (EditText)findViewById(R.id.et_red_light);
        et_green_light = (EditText)findViewById(R.id.et_green_light);
        et_number = (EditText)findViewById(R.id.et_number);
        
        tv_red = (TextView)findViewById(R.id.tv_red);
        tv_green = (TextView)findViewById(R.id.tv_green);
        
        DisplayMetrics metrics = new DisplayMetrics();  
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        ////////////////////////program layout//////////////
        LinearLayout ll_light = (LinearLayout)findViewById(R.id.ll_light);
        
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) (((double)20/100)*metrics.widthPixels));
        
        ll_light.setLayoutParams(params);
        
        params = new LayoutParams((int) (((double)25/100)*metrics.widthPixels),LayoutParams.MATCH_PARENT);
        
        tv_red.setLayoutParams(params);
        tv_red.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) (((double)15/100)*metrics.widthPixels));
        tv_green.setLayoutParams(params);
        tv_green.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) (((double)15/100)*metrics.widthPixels));
        
        params = new LayoutParams((int) (((double)25/100)*metrics.widthPixels),LayoutParams.MATCH_PARENT);
        
        TextView tv1 = (TextView)findViewById(R.id.tv1);
        tv1.setLayoutParams(params);
        
		params = new LayoutParams((int) (((double)15/100)*metrics.widthPixels),LayoutParams.MATCH_PARENT);
        
        TextView tv2 = (TextView)findViewById(R.id.tv2);
        tv2.setLayoutParams(params);
        //////////////////////////////////
        
        df = new DecimalFormat("0.0");   
        String volume = df.format(THLApp.volume);  
 	            
        //deviceBTMac = mBTAdapter.getAddress();    
        
        et_red_light.setText(THLApp.red_light);
        et_green_light.setText(THLApp.green_light);
        et_number.setText(THLApp.number);
        tv_volume.setText(volume);
        
        mp.setLooping(false);
        //mp.setVolume(1.0f, 1.0f);
        mp.setVolume(THLApp.volume, THLApp.volume);       
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        //mp.start();
        
        //sp.play(sp_Id,1.0f,1.0f,0,0,0.1f);
        
        light_time = Integer.valueOf(THLApp.green_light);
        
        //mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_MAC, 3000);   
        mHandler.sendEmptyMessageDelayed(Constants.MSG_DELETE_AW_FILE, 1);
        mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,40);
        mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD,1000);
        
        TestLED testLED = new TestLED();
        testLED.start();

    }
    /** ================================================ */
   	@Override
   	public void onResume()
	{
		super.onResume();
		usbSerialPortManager.USBintial();
		//Log.d("debug", "onResume");
	}
    /** ================================================ */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		usbSerialPortManager.SendCMD("stop_scan\n",0);
		usbSerialPortManager.changeBeacon(0,0,0,0,0);
		if(usbSerialPortManager.getDeviceSize() >1)
		{
			usbSerialPortManager.SendCMD("stop_scan\n",1);
			usbSerialPortManager.changeBeacon(0,0,0,0,1);
		}
		usbSerialPortManager.isOurDevice = false;
		usbSerialPortManager.unregisterReceiver();
		mHandler.removeMessages(Constants.MSG_SHOW_LIGHT);
		mHandler.removeMessages(Constants.MSG_RECEIVE);
		mHandler.removeMessages(Constants.MSG_UPLOAD);
		mHandler.removeMessages(Constants.MSG_SHOW_TIME);
		mHandler.removeMessages(Constants.MSG_DELETE_AW_FILE);
		System.exit(0);
	}
	
    /** ================================================ */
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id= v.getId();
    	
    	switch(id)
    	{
    		case R.id.btn_save:
    			if(!et_red_light.getText().toString().trim().equals("") 
    			|| !et_green_light.getText().toString().trim().equals(""))
    			{
    				if(Integer.valueOf(et_red_light.getText().toString().trim()) >= 255)
    				{
    					THLApp.red_light = "255";
    					et_red_light.setText("255");
    				}
    				else
    					THLApp.red_light = et_red_light.getText().toString().trim();
    				
    				if(Integer.valueOf(et_green_light.getText().toString().trim()) >= 255)
    				{
    					THLApp.green_light = "255";
    					et_green_light.setText("255");
    				}
    				else
    					THLApp.green_light = et_green_light.getText().toString().trim();
    				
    				if(Integer.valueOf(et_number.getText().toString().trim()) >= 65535)
    				{
    					THLApp.number = "65535";
    					et_number.setText("65535");
    				}
    				else
    					THLApp.number = et_number.getText().toString().trim();
	
    				THLApp.saveSettings();
    				
    				light_time = Integer.valueOf(THLApp.green_light);
    				
    				tv_red.setText("");
    				
    				//usbSerialPortManager.SendCMD("stop_scan\n");
    				
    				String OutputStirng = "";
    		 		
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
    				
    		 		if(!usbSerialPortManager.isReceiver[0])
    		 		{
    		 			usbSerialPortManager.SendCMD(OutputStirng,0);
    		 		}
    		 		if(!usbSerialPortManager.isReceiver[1])
    		 		{
	    				if(usbSerialPortManager.getDeviceSize() >1)
	    				{
	    					usbSerialPortManager.SendCMD(OutputStirng,1);
	    				}
    		 		}
    				
    				tv_green.setBackgroundResource(R.drawable.green_light);
    				tv_red.setBackgroundResource(R.drawable.gray_light);
    				
    				isRed = false;
    				
    				mHandler.removeMessages(Constants.MSG_SHOW_LIGHT);
    				mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_LIGHT,1000);
    				
    				tv.setText("Save success");
    			}
    			else
    			{
    				runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(UIMain.this, "欄位不得為空", Toast.LENGTH_LONG).show();
						}
					});
    			}
    			break;
    		case R.id.btn_add_volume:
    			if(THLApp.volume == 1)
    				return;
    			else
    			{
    				THLApp.volume = (float) (THLApp.volume + 0.1);
    				if(THLApp.volume > 1)
    					THLApp.volume = 1;
    				mp.setVolume(THLApp.volume, THLApp.volume);
    				THLApp.saveSettings();
    				
    				String volume = df.format(THLApp.volume);
    				
    				tv_volume.setText(volume);
    			}
    			break;
    		case R.id.btn_reduce_volume:
    			if(THLApp.volume == 0)
    				return;
    			else
    			{
    				THLApp.volume = (float) (THLApp.volume - 0.1);
    				if(THLApp.volume < 0)
    					THLApp.volume = 0;
    				mp.setVolume(THLApp.volume, THLApp.volume);
    				THLApp.saveSettings();
    				
    				String volume = df.format(THLApp.volume);
    				
    				tv_volume.setText(volume);
    			}
    			break;
    		case R.id.btn_min:
    			THLApp.volume = 0.3f;
    			mp.setVolume(THLApp.volume, THLApp.volume);
				THLApp.saveSettings();
				
				String volume = df.format(THLApp.volume);
				
				tv_volume.setText(volume);
    			break;
    		case R.id.btn_max:
    			THLApp.volume = 1.0f;
    			mp.setVolume(THLApp.volume, THLApp.volume);
				THLApp.saveSettings();
				
				String volume2 = df.format(THLApp.volume);
				
				tv_volume.setText(volume2);
    			break;
    	}
	}
	
	
	/***************************************************************/
	public static ArrayList<BeaconInfo> cloneList(ArrayList<BeaconInfo> list) {
		ArrayList<BeaconInfo> clone = new ArrayList<BeaconInfo>(list.size());
	    for(BeaconInfo item: list) clone.add(item.clone());
	    return clone;
	}
//	/*****************************************************/
//	@SuppressWarnings("deprecation")
//	public void httpUpdateDevice(String url,List<NameValuePair> params)
//	{
//				
//		String strResult 	= "";
//		
//		String body;
//		
//        HttpPost post = new HttpPost(url);
//    
//        try {
//        	
//        	post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
//        	//post.addHeader("content-type", "application/json");
//            HttpParams httpParameters = new BasicHttpParams();
//            HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
//            HttpConnectionParams.setSoTimeout(httpParameters, 10000);
//            
//            HttpResponse httpResponse = new DefaultHttpClient(httpParameters).execute(post);
//            
//            if(httpResponse.getStatusLine().getStatusCode()==200){
//                
//                strResult = EntityUtils.toString(httpResponse.getEntity());
//                Log.d("debug", ""+httpResponse.getStatusLine().getStatusCode()+",result="+strResult);
//                mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, Integer.valueOf(THLApp.upload_time));
//				mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
//            }
//            else
//            {
//            	strResult = EntityUtils.toString(httpResponse.getEntity());
//            	Log.d("debug", ""+httpResponse.getStatusLine().getStatusCode()+",result="+strResult);
//            	mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, Integer.valueOf(THLApp.upload_time));
//				mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
//            }
//        } catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, Integer.valueOf(THLApp.upload_time));
//			mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
//		} catch (ClientProtocolException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, Integer.valueOf(THLApp.upload_time));
//			mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, Integer.valueOf(THLApp.upload_time));
//			mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
//		}
//
//	}
	
	/** ===================================================================================== */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		// TODO Auto-generated method stub		
		
		saveErrorLog(ex);
		usbSerialPortManager.SendCMD("stop_scan\n",0);
		usbSerialPortManager.changeBeacon(0,0,0,0,0);
		if(usbSerialPortManager.getDeviceSize() >1)
		{
			usbSerialPortManager.SendCMD("stop_scan\n",1);
			usbSerialPortManager.changeBeacon(0,0,0,0,1);
		}
		
		//usbSerialPortManager.SendCMD("stop_scan\n");
		
		PendingIntent intent = PendingIntent.getActivity(UIMain.this, 0,
	            new Intent(getIntent()), getIntent().getFlags());
		AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
		System.exit(2);
	}
	
	/**======================================================================================**/
	public boolean saveErrorLog(Throwable ex)
	{
		Date curDate = new Date(System.currentTimeMillis());
			
		
		String SavePath = THLApp.STORE_PATH + "error_log.txt";
		
		Writer writer = new StringWriter();  
        PrintWriter printWriter = new PrintWriter(writer);  
        ex.printStackTrace(printWriter);  
        Throwable cause = ex.getCause();  
        while (cause != null) {  
            cause.printStackTrace(printWriter);  
            cause = cause.getCause();  
        }  
        printWriter.close();  
        String result = writer.toString();  
        
        Log.d("debug","crash:"+result);
		
		try {
			FileWriter fw = new FileWriter(SavePath,true);
		   	BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
  	        bw.write(formatter.format(curDate)+":"+result);
  	        bw.newLine();
  	        bw.close();  
  	        return true;
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		   e.printStackTrace();
	    }
		return false;		
	}
	/**======================================================================================*/
	public void parseBeaconDataToList()
	{
		String[] receivers = MessageString.split("\n");
//		Log.d("debug", "==========================");
//		Log.d("debug", MessageString);
//		Log.d("debug", "==========================");
		for(int i = 0 ; i < receivers.length ; i++)
		{
			String data = receivers[i].replace("\r", "");
			if(data.length() == 72 || data.length() == 84 || data.length() == 71)
			{
				BeaconInfo beaconInfo = new BeaconInfo();
				Date curDate = new Date(System.currentTimeMillis());
				
				beaconInfo.time = formatter.format(curDate);
				beaconInfo.scanned_mac = data.substring(0, 17);
				try {
					if(data.length() == 72)
					{
						beaconInfo.major = String.valueOf(Integer.parseInt(data.substring(55, 59),16));
						beaconInfo.minor = String.valueOf(Integer.parseInt(data.substring(60, 64),16));
						beaconInfo.rssi = data.substring(69, 72);	
						beaconInfo.uuid = data.substring(18, 54);	
					}
					else if(data.length() == 71)
					{
						beaconInfo.major = String.valueOf(Integer.parseInt(data.substring(55, 59),16));
						beaconInfo.minor = String.valueOf(Integer.parseInt(data.substring(60, 64),16));
						beaconInfo.rssi = data.substring(68, 71);	
						beaconInfo.uuid = data.substring(18, 54);	
					}
					else if(data.length() == 84){
						beaconInfo.rssi = data.substring(81, 84);
						beaconInfo.uuid = data.substring(19, 78);
					}			
				} catch (Exception e) {
					// TODO: handle exception
					Log.d("debug", "major or minor not integer");
					continue;
				}	
				
				if(!isMac(beaconInfo.scanned_mac))
				{
					Log.d("debug", beaconInfo.scanned_mac+" is not mac");
					continue;
				}
//				if(!THLApp.isNumeric(beaconInfo.major))
//					return;
//				if(!THLApp.isNumeric(beaconInfo.minor))
//					return;
				if(!THLApp.isNumeric(beaconInfo.rssi))
				{
					Log.d("debug", "rssi not integer");
//					receivers = null;
//					MessageString = "";
					continue;
				}
				
				if(!beaconInfo.uuid.equals("436DFAB4-03AF-4F10-A039-4503BB94BD56"))
				{
					continue;
				}

				//Log.d("debug", THLApp.uuid + "," + beaconInfo.uuid + ",pass");
				/********************************************************************************/
				if(!beaconInfo.rssi.equals(""))
				{
					if(beaconList.isEmpty())
					{
						try {
							beaconInfo.rssiList.add(Double.valueOf(beaconInfo.rssi));
							beaconInfo.count++;
							beaconList.add(beaconInfo);
						} catch (Exception e) {
							// TODO: handle exception
							Log.d("debug", e.toString());
						}	
					}
					else 
					{
						count = 0;
						for(int j = 0; j<beaconList.size(); j++)
						{
							if(beaconList.get(j).scanned_mac.equals(beaconInfo.scanned_mac))
							{		
								try {
									beaconList.get(j).add(Double.valueOf(beaconInfo.rssi));
									beaconList.get(j).time = beaconInfo.time;
									beaconList.get(j).rssi = beaconInfo.rssi;
									beaconList.get(j).count++;
	
									beaconList.get(j).avgRssi(1);
									beaconList.set(j, beaconList.get(j));
								} catch (Exception e) {
									// TODO: handle exception
									Log.d("debug", e.toString());
								}	
								break;
							}
							count++;
						}					
					
						if(count == beaconList.size()) 
						{
							try {
								beaconInfo.rssiList.add(Double.valueOf(beaconInfo.rssi));
								beaconInfo.count++;
								beaconList.add(beaconInfo);
							} catch (Exception e) {
								// TODO: handle exception
								Log.d("debug", e.toString());
							}	
						}
					}
				}
			}
			
		}
		receivers = null;
		MessageString = "";
		str1 = "";
		str2 = "";
	}
	/*************************************************/
	public boolean isReceiver(String str)
	{
		if(str.contains("command not found"))
		{
			return false;
		}
		return true;
	}
	/***************************************/
	private boolean isMac(String val) {  
        String trueMacAddress = "([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}";  
        // 这是真正的MAV地址；正则表达式；  
        if (val.matches(trueMacAddress)) {  
            return true;  
        } else {  
            return false;  
        }  
    }  
//	/**======================================================================================
//	 * @throws JSONException */
//	public void parseTcpipData(String tcpipString) throws JSONException
//	{
//		JSONObject jsonObject = new JSONObject(tcpipString);
//		
//		int cmd = 0;
//		if(!jsonObject.isNull("cmd"))
//			cmd = jsonObject.getInt("cmd");
//		
//		switch (cmd)
//		{
//			case Constants.TCPIP_SET_UPLOAD_TIME:
//				String upload_time = "";
//				if(!jsonObject.isNull("upload_time"))
//				{
//					upload_time = jsonObject.getString("upload_time");
//					et_upload_time.setText(upload_time);
//					btn_save.performClick();
//				}
//				break;
//			case Constants.TCPIP_SET_SCAN_TIME:
//				String start_scan_time = "";
//				String stop_scan_time = "";
//				if(!jsonObject.isNull("start_scan_time"))
//				{
//					start_scan_time = jsonObject.getString("start_scan_time");
//					et_scan_time.setText(start_scan_time);
//				}
//				if(!jsonObject.isNull("stop_scan_time"))
//				{
//					stop_scan_time = jsonObject.getString("stop_scan_time");
//					et_stop_scan_time.setText(stop_scan_time);
//				}
//				if(!start_scan_time.equals("") && !stop_scan_time.equals(""))
//				{
//					btn_save.performClick();
//				}
//				break;
//			case Constants.TCPIP_SET_SERVER_TYPE_AND_URL:
//				String server_type = "";
//				String url = "";
//				if(!jsonObject.isNull("server_type"))
//				{
//					server_type = jsonObject.getString("server_type");
//					for(int i = 0 ; i < rg.getChildCount() ; i++)
//			        {
//			        	RadioButton rb = (RadioButton) rg.getChildAt(i);
//			        	if(rb.getText().toString().equals(server_type))
//			        	{
//			        		rb.setChecked(true);
//			        		break;
//			        	}
//			        }
//				}
//				if(!jsonObject.isNull("url"))
//				{
//					url = jsonObject.getString("url");
//					et_url.setText(url);
//				}
//				if(!server_type.equals(""))
//				{
//					btn_save.performClick();
//				}
//				break;
//			case Constants.TCPIP_SET_ALGORITHM:
//				String algorithm = "";
//				if(!jsonObject.isNull("algorithm"))
//				{
//					algorithm = jsonObject.getString("algorithm");
//					for(int i = 0 ; i < rg_avg.getChildCount() ; i++)
//			        {
//			        	RadioButton rb = (RadioButton) rg_avg.getChildAt(i);
//			        	if(rb.getText().toString().equals(algorithm))
//			        	{
//			        		rb.setChecked(true);
//			        		break;
//			        	}
//			        }
//				}
//				if(!algorithm.equals(""))
//				{
//					btn_save.performClick();
//				}
//				break;
//			case Constants.TCPIP_SET_UUID_FILTER:
//				String uuid_filter = "";
//				if(!jsonObject.isNull("uuid_filter"))
//				{
//					uuid_filter = jsonObject.getString("uuid_filter");
//					et_filter_uuid.setText(uuid_filter);
//					btn_save.performClick();
//				}
//				break;
//			case Constants.TCPIP_SET_SSID_AND_KEY:
//				String ssid = "";
//				String key = "";
//				if(!jsonObject.isNull("ssid"))
//				{
//					ssid = jsonObject.getString("ssid");
//					et_ssid.setText(ssid);
//				}
//				if(!jsonObject.isNull("key"))
//				{
//					key = jsonObject.getString("key");
//					et_key.setText(key);
//				}
//				if(!ssid.equals("") && !key.equals(""))
//				{
//					btn_save.performClick();
//				}
//				break;
//			case Constants.TCPIP_SET_TIME:
//				String time = "";
//				if(!jsonObject.isNull("time"))
//				{
//					time = jsonObject.getString("time");
//					String[] times = time.split("-");
//					timeManager.changeSystemTime(times[0],times[1],times[2],times[3],times[4],times[5]);
//					changeUITime();
//					//tv_time.setText(time);
//					//btn_save.performClick();
//				}
//				break;
//		}
//	}
//	
//	/**======================================================================================*/
//	public void parseBeaconDataToList()
//	{
//		String[] receivers = MessageString.split("\n");
////		Log.d("debug", "==========================");
////		Log.d("debug", MessageString);
////		Log.d("debug", "==========================");
//		for(int i = 0 ; i < receivers.length ; i++)
//		{
//			String data = receivers[i].replace("\r", "");
//			if(data.length() == 72 || data.length() == 84)
//			{
//				BeaconInfo beaconInfo = new BeaconInfo();
//				Date curDate = new Date(System.currentTimeMillis());
//				
//				beaconInfo.time = formatter.format(curDate);
//				beaconInfo.scanned_mac = data.substring(0, 17);
//				try {
//					if(data.length() == 72)
//					{
//						beaconInfo.major = String.valueOf(Integer.parseInt(data.substring(55, 59),16));
//						beaconInfo.minor = String.valueOf(Integer.parseInt(data.substring(60, 64),16));
//						beaconInfo.rssi = data.substring(69, 72);	
//						beaconInfo.uuid = data.substring(18, 54);	
//					}
//					else if(data.length() == 84){
//						beaconInfo.rssi = data.substring(81, 84);
//						beaconInfo.uuid = data.substring(19, 78);
//					}			
//				} catch (Exception e) {
//					// TODO: handle exception
//					Log.d("debug", "major or minor not integer");
//					continue;
//				}
//				
////				if(!THLApp.isNumeric(beaconInfo.major))
////					return;
////				if(!THLApp.isNumeric(beaconInfo.minor))
////					return;
//				if(!THLApp.isNumeric(beaconInfo.rssi))
//				{
//					Log.d("debug", "rssi not integer");
////					receivers = null;
////					MessageString = "";
//					continue;
//				}
//				if(!THLApp.uuid.trim().equals(""))
//				{
//					if(!THLApp.uuid.equals(beaconInfo.uuid))
//					{
//						//Log.d("debug", THLApp.uuid + "," + beaconInfo.uuid);
////						receivers = null;
////						MessageString = "";
//						continue;
//					}
//				}
//				//Log.d("debug", THLApp.uuid + "," + beaconInfo.uuid + ",pass");
//				/********************************************************************************/
//				if(!beaconInfo.rssi.equals(""))
//				{
//					if(beaconList.isEmpty())
//					{
//						try {
//							beaconInfo.rssiList.add(Double.valueOf(beaconInfo.rssi));
//							beaconInfo.count++;
//							beaconList.add(beaconInfo);
//						} catch (Exception e) {
//							// TODO: handle exception
//							Log.d("debug", e.toString());
//						}	
//					}
//					else 
//					{
//						if(THLApp.Algorithm.equals("AVG"))
//						{
//							count = 0;
//							for(int j = 0; j<beaconList.size(); j++)
//							{
//								if(beaconList.get(j).scanned_mac.equals(beaconInfo.scanned_mac))
//								{		
//									try {
//										beaconList.get(j).add(Double.valueOf(beaconInfo.rssi));
//										beaconList.get(j).time = beaconInfo.time;
//										beaconList.get(j).rssi = beaconInfo.rssi;
//										beaconList.get(j).count++;
//		
//										beaconList.get(j).avgRssi(1);
//										beaconList.set(j, beaconList.get(j));
//									} catch (Exception e) {
//										// TODO: handle exception
//										Log.d("debug", e.toString());
//									}	
//									break;
//								}
//								count++;
//							}					
//						
//							if(count == beaconList.size())
//							{
//								try {
//									beaconInfo.rssiList.add(Double.valueOf(beaconInfo.rssi));
//									beaconInfo.count++;
//									beaconList.add(beaconInfo);
//								} catch (Exception e) {
//									// TODO: handle exception
//									Log.d("debug", e.toString());
//								}	
//							}
//						}
//						else
//						{
//							try {
//								beaconInfo.rssiList.add(Double.valueOf(beaconInfo.rssi));
//								beaconInfo.count++;
//								beaconList.add(beaconInfo);
//							} catch (Exception e) {
//								// TODO: handle exception
//								Log.d("debug", e.toString());
//							}	
//						}
//					}
//				}
//			}
//			
//		}
//		receivers = null;
//		MessageString = "";
//	}
//	/***********************************************************************************************/
//	public void SendCustomizeDataToServer()
//	{
//		Date curDate = new Date(System.currentTimeMillis());
//		
//    	JSONArray jsArray = new JSONArray();
//    	
//    	for(int i = 0 ; i < tempList.size(); i++)
//    	{
//    		JSONObject object = new JSONObject();		                
//
//    		try {
//				object.put("scanned_ble_mac", tempList.get(i).scanned_mac);
//				object.put("ble_mac", deviceBTMac);
//        		object.put("uuid", tempList.get(i).uuid);
//        		object.put("major", tempList.get(i).major);
//        		object.put("minor", tempList.get(i).minor);
//        		object.put("rssi", tempList.get(i).rssi);
//        		object.put("scan_time", tempList.get(i).time);
//        		object.put("sentTime", formatter.format(curDate));
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}		                    	
//    		jsArray.put(object);
//    	}
//    	List<NameValuePair> params = new ArrayList<NameValuePair>();				        
//    	params.add(new BasicNameValuePair("act", "receiver"));
//    	params.add(new BasicNameValuePair("data", jsArray.toString()));
//    	httpUpdateDevice(THLApp.url,params);    
//	}
//	/***********************************************************************/
//	public void SendIScanDataToServer()
//	{
//		Date curDate = new Date(System.currentTimeMillis());
//		
//		JSONArray jsArray = new JSONArray();
//		
//		for(int i = 0 ; i < tempList.size(); i++)
//		{
//			JSONObject object = new JSONObject();		                
//	
//			try {
//				object.put("lon", "-1");
//				object.put("lat", "-1");
//	    		object.put("userId", "");
//	    		object.put("enterTime", tempList.get(i).time);
//	    		object.put("wifiId", WifiMac);
//	    		object.put("beaconId", "");
//	    		object.put("deviceId", tempList.get(i).scanned_mac);
//	    		object.put("rssi", tempList.get(i).rssi);
//	    		object.put("sentTime", formatter.format(curDate));
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}		                    	
//			jsArray.put(object);
//		}
//		List<NameValuePair> params = new ArrayList<NameValuePair>();				        
//		params.add(new BasicNameValuePair("data", jsArray.toString()));
//		httpUpdateDevice(iScanUploadUrl,params);    
//	}
	
//	/******************************* 改變時間 **********************************/
//    private void changeUITime()
//    {
//    	dc.setFormat24Hour("yyyy-MM-dd hh:mm, EEEE");
//        tv_time.setText(dc.getText().toString());
//        mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_TIME,5000);
//    }
	
    
}
