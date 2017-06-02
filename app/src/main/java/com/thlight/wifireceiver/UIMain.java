package com.thlight.wifireceiver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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

import com.google.android.things.pio.PeripheralManagerService;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class UIMain extends Activity implements View.OnClickListener , UncaughtExceptionHandler{

    /*====================================================================*/
    final String UART_PATH1                           = "/dev/ttyS2";
    final String UART_PATH2                           = "/dev/ttyS3";
    final int BAUD_RATE                               = 115200;
    final int RECORD_VOLUME_FREQUENCY            = 1000;       // 1S  記錄背景聲音的頻率
    final int CHANGE_VOLUME_FREQUENCY            = 1;         //  根據背景音量而改變聲音 的頻率 (以紅綠燈變化一次為一週期)
    final int REFERENCE_TIMES                         = 3;          // 在 CHANGE_VOLUME_FREQUENCY 內會去取 3 次的聲音平均
    final int RECEIVE_FREQUENCY                      = 40;        //0.04S
    final int UPLOAD_FREQUENCY                      = 2000;      //2S
    final double EXTREMELY_VALUE_RANGE            = 0.1;      //10% for up and down
    final int SHOW_LIGHT_FREQUENCY                 = 1000;      //每秒改變號誌秒數   //test
    final int GREEN_LIGHT_ENDING                     = 15;        //當綠燈剩 15秒時, 提示音每秒都響
    final int SAMPLE_RATE_IN_HZ                       = 8000;
    final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    final int UPLOAD_TURNS                             = 5;       //To reset u32UploadCount and fMaxVolume every 5 upload times.

    final float fVolumeLevel1 = 0.2f;                // For setting volume.
    final float fVolumeLevel2 = 0.5f;
    final float fVolumeLevel3 = 0.8f;
    final float fVolumeLevel4 = 1.0f;

    final int u32BgNoiseLevel1 = 65;
    final int u32BgNoiseLevel2 = 75;
    boolean isGetVoiceRun     = false;
    int u32UploadCount        = 0;                          //對於多人連線時, 十秒內取最大值
    float fMaxVolume          = 0;                            // 十秒內最大的調整音量
    int u32RedTime = 0;
    int u32GreenTime = 0;
    int u32PreRedTime = 0;
    int u32PreGreenTime = 0;
    Timer GT = new Timer();
    Timer RT = new Timer();
    boolean GTPause = true;
    boolean RTPause = true;
    boolean bFirstRound = true;
    ArrayList<Double> aAverageVolume = new ArrayList<Double>();      // Declare a array to record the average volume.
    ArrayList<Integer> aReceiverRecord = new ArrayList<Integer>();    //記錄 receiver 是在哪個USB位置

    Boolean bShowLightStart = false;        //The show light status.
    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    ScheduledFuture<?> scheduledFuture ;   // For cancel the ShowLight task.
    Boolean bAudioOn = true;
    double dVolumeAverage = 0;

    String Data = "";

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

    class ShowLight implements Runnable {

        @Override
        public void run() {

            try {

                String OutputString = "";

                /*Counting the traffic time.*/
                if (!GTPause)
                    u32GreenTime++;
                if (!RTPause)
                    u32RedTime++;
                /*
                int A = -1;
                FileReader gpioExport;
                gpioExport = new FileReader("/sys/class/gpio_sw/PA16/data");
                //A = gpioExport.read();
                gpioExport.close();
                */
/*
                Scanner scanner = new Scanner(new File("/sys/class/gpio_sw/PA20/data"));

                if (scanner.nextInt() == 1)
                {
                    THLApp.bEastWest = true;
                }
                else
                {
                    THLApp.bEastWest = false;
                }

                scanner = new Scanner(new File("/sys/class/gpio_sw/PA21/data"));

                if (scanner.nextInt() == 1)
                {
                    THLApp.bSouthNorth = true;
                    //isRed = true;
                }
                else
                {
                    THLApp.bSouthNorth = false;
                    //isRed = false;
                }
*/

                //After know the traffic time at first round, start to show light time.
                if (!bFirstRound)
                {
                    light_time = light_time-1;

                    //Show light time on the screen.
                    runOnUiThread(new Runnable() {
                        public void run() {

                            if(light_time == 0 || light_time < 0)
                            {
                                sp.setRate(sp_Id, 1.0f);
                                isRed = !isRed;
                                if(isRed)
                                {
                                    //light_time = Integer.valueOf(THLApp.red_light);
                                    light_time = u32PreRedTime;
                                    tv_green.setBackgroundResource(R.drawable.gray_light);
                                    tv_green.setText("");
                                    tv_red.setBackgroundResource(R.drawable.red_light);
                                }
                                else
                                {
                                    //light_time = Integer.valueOf(THLApp.green_light);
                                    light_time = u32PreGreenTime;
                                    tv_green.setBackgroundResource(R.drawable.green_light);
                                    tv_red.setBackgroundResource(R.drawable.gray_light);
                                    tv_red.setText("");
                                }
                            }

                            if(isRed)
                                tv_red.setText(""+light_time);
                            else
                                tv_green.setText(""+light_time);

                            tv.setText("time:"+light_time);
                        }
                    });

                    String LightHexTime = "";

                    String number = Integer.toHexString((Integer.valueOf(THLApp.number)));

                    // 紅綠燈編號要補成 4 bytes. (Major 位置)
                    if(number.length() == 3)
                        number = "0"+number;
                    else if(number.length() == 2)
                        number = "00"+number;
                    else if(number.length() == 1)
                        number = "000"+ number;//(int) dVolumeAverage;

                /*========Chang the ref RSSI will change the value of RSSI.==================================================*/
                    //Volume 設成 reference RSSI (2 bytes).
                    String sVolume = Integer.toHexString(((int) (THLApp.volume * 10)));
                    Log.d("debug", "sVolume: " + sVolume + " THLApp.volume : " + THLApp.volume);
                    if(sVolume.length() == 1)
                    {
                        sVolume = "0"+ sVolume;
                    }
                /*==========Chang the ref RSSI will change the value of RSSI.================================================*/

                    /*Green light.  Minor is light time.*/
                    if(!isRed)
                    {
                        LightHexTime = Integer.toHexString(light_time + 1*256);//綠燈 = 256 + 現在的倒數時間

                        if(LightHexTime.length() == 4)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" "+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                        else if(LightHexTime.length() == 3)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 0"+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                        else if(LightHexTime.length() == 2)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 00"+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                        else if(LightHexTime.length() == 1)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 000"+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                    }
                    else {
                        LightHexTime = Integer.toHexString(light_time + 2*256);//紅燈 = 2*256 + 現在的倒數時間

                        if(LightHexTime.length() == 4)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" "+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                        else if(LightHexTime.length() == 3)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 0"+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                        else if(LightHexTime.length() == 2)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 00"+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                        else if(LightHexTime.length() == 1)
                            OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 000"+LightHexTime+" "+THLApp.REF_RSSI+"\n";
                    }

                    if(light_time <= GREEN_LIGHT_ENDING && !isRed && bAudioOn)
                    {
                        mp.start();
                    }
                    else if(light_time%2 == 0 && !isRed && bAudioOn)
                    {
                        mp.start();
                    }
                }

                //Log.d("debug", "isRed:"+isRed + "time:"+light_time);

                //Log.d("debug", "OutputString : " + OutputString);
                // Send command to Beacon.
                for (int i = 0; i< usbSerialPortManager.getDeviceSize(); i++)
                {
                    if (!com.thlight.wifireceiver.usbSerialPortManager.aIsReceiver[i]) {
                        Log.d("debug", "bEastWest: " + THLApp.bEastWest + " bSouthNorth: " + THLApp.bSouthNorth + " OutputString : " + OutputString);
                        usbSerialPortManager.SendCMD(OutputString, i);
                        //usbSerialPortManager.SendCMD(THLApp.GET_KEEP_SETTING, i);

                        //Check GPIO

                        CheckGPIO(i);
                    }
                }


            }
            catch (Throwable e)
            {
                Log.d("debug", "Show Light fail.");
            }
        }
    }

    Handler mHandler= new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
                /*Get the data from the receiver continually.*/
				case Constants.MSG_RECEIVE:

                    // Create threads for each receiver to get data from receiver.
                    for (int i =0; i< aReceiverRecord.size(); i++)
                    {
                        //Log.d("5566", "aIsReceiver[" + aReceiverRecord.get(i)+ "] : " + com.thlight.wifireceiver.usbSerialPortManager.aIsReceiver[aReceiverRecord.get(i)]);
                        final int finalI = i;
                        new Thread(new Runnable() {
                                public void run() {
                                    byte[] buf = new byte[2000];
                                    int re = usbSerialPortManager.getSerialPortData(buf, aReceiverRecord.get(finalI));

                                    if(re != -1)
                                    {
                                        for(int j = 0 ; j< re ;j++)
                                        {
                                            str1 +=(char)buf[j];
                                        }

                                        if(MessageString.length()<500000)
                                            MessageString = MessageString + new String(str1);
                                            //Log.d("usbManager", "usb1:true"+","+MessageString.length());

                                        if(finalI == 0)
                                        {
                                           // Log.d("hebe1", "str: " + str1);
                                        }
                                        else
                                        {
                                            //Log.d("hebe2", "str1: " + str1);
                                        }

                                    }
                                    str1 = "";
                                    buf = null;

                                }
                            }).start();
                    }

                    mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE, RECEIVE_FREQUENCY);

					break;
				case Constants.MSG_CONNECT_SUCCESS:
					tv.setText("connect success");
					break;
                /*Start to run ShowLight*/
				case Constants.MSG_SHOW_LIGHT:
					Log.d("debug", "Enter MSG_SHOW_LIGHT");

                    if (!bShowLightStart) {
                        // Start upload task repeatedly  every THLApp.upload_time.
                        // SHOW_LIGHT_FREQUENCY : the period between successive executions
                        scheduledFuture = exec.scheduleAtFixedRate(new ShowLight(), SHOW_LIGHT_FREQUENCY, SHOW_LIGHT_FREQUENCY, TimeUnit.MILLISECONDS);
                        bShowLightStart = true;
                    }
                    else
                    {
                        //do nothing.
                    }
					break;
                /*解析receiver收到的資料 然後把它存成list, 去調音量, 每兩秒執行一次*/
				case Constants.MSG_UPLOAD:
                    Log.d("debug", "Enter MSG_UPLOAD");
					mHandler.removeMessages(Constants.MSG_RECEIVE);     //解析資料時不收資料
					mHandler.removeMessages(Constants.MSG_UPLOAD);
					parseBeaconDataToList();                               //
					tempList = null;
					tempList = cloneList(beaconList);
					beaconList.clear();
                    int u32TempListSize = tempList.size();
					//Log.d("debug", "beacon size:"+tempList.size());

                    //Start to record audio volume when receiving beacon list is 0.
                    if (u32TempListSize == 0)
                    {
                        // Restart to record the audio volume.
                        StartRecordAudioVolume();
                        //=====(Optional) Turn off the audio.============================//
                        //bAudioOn = false;

                        //Start show light after 同步
                        if (THLApp.DEMO && !bShowLightStart)
                        {
                            scheduledFuture = exec.scheduleAtFixedRate(new ShowLight(), SHOW_LIGHT_FREQUENCY, SHOW_LIGHT_FREQUENCY, TimeUnit.MILLISECONDS);
                            bShowLightStart = true;
                        }

                    }
                    else
                    {
                        for(int i = 0 ; i < u32TempListSize ; i++)
                        {
                            Log.d("beacon", "major:"+tempList.get(i).major + ",minor:"+tempList.get(i).minor);
                            //Major 調音量
                            THLApp.volume = Float.valueOf(tempList.get(i).major)/100;
                            tv_volume.setText(THLApp.volume + "");

                            /*Setting volume if THLApp.volume is bigger than fMaxVolume*/
                            if (THLApp.volume >= fMaxVolume)
                            {
                                fMaxVolume = THLApp.volume;
                                // 0.0 ~ 1.0
                                mp.setVolume(THLApp.volume, THLApp.volume);
                                u32UploadCount = 0;  // Set counting 0 after setting the volume.
                            }
                            else
                            {
                                THLApp.volume = fMaxVolume;
                            }
                            isGetVoiceRun = false;     // Stop getting background noise.

                            /*Start to announce after the user is closing.*/
                            if(!tempList.get(i).major.equals("0"))
                            {
                                //bAudioOn = true;
                            }
                            String sMinor = tempList.get(i).minor;
                            String sMajor = tempList.get(i).major;
                            // 同步時間, only for demo. The value of minor is for the traffic light time.
                            if (THLApp.DEMO && !sMinor.equals("0") && sMajor.equals("0"))
                            {
                                Log.d("terry", "tempList.get(i).minor : " + tempList.get(i).minor + " i : " + i);
                                isRed = false;
                                light_time = Integer.valueOf(sMinor);

                                tv_green.setBackgroundResource(R.drawable.green_light);
                                tv_red.setBackgroundResource(R.drawable.gray_light);
                                tv_red.setText("");
                                //Stop show light when 同步
                                scheduledFuture.cancel(false);
                                bShowLightStart = false;
                            }
                        }
                    }

                    u32UploadCount++;
                    // Reset u32UploadCount and fMaxVolume every 10 seconds.
                    // 當同時收到好幾筆調聲音的需求時, 取最大的, 存到 fMaxVolume
                    // 維持十秒後, 重新記錄聲音最大值
                    if (u32UploadCount == UPLOAD_TURNS)
                    {
                        u32UploadCount = 0;
                        fMaxVolume = 0;
                    }

					mHandler.sendEmptyMessageDelayed(Constants.MSG_RECEIVE,10);
					mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, UPLOAD_FREQUENCY);

					break;
                /*Delete the log files that create by Banana Pi  for each hour. */
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
                //記錄哪根是 receiver, beacon.
                case Constants.MSG_RECOGNIZE_RECEIVER:

                    for (int i = 0; i<usbSerialPortManager.getDeviceSize(); i++)
                    {
                        byte[] buf = new byte[2000];
                        int re = usbSerialPortManager.getSerialPortData(buf, i);

                        if(re != -1)
                        {
                            for(int j = 0 ; j< re ;j++)
                            {
                                str1 +=(char)buf[j];
                            }
                            //tv.setText(MessageString);
                            //看是否有 command not found 字眼, 沒有就是 RECEIVER.
                            if(isReceiver(str1))
                            {
                                if(MessageString.length()<500000)
                                    MessageString = MessageString + new String(str1);
                                //Log.d("usbManager", "usb1:true"+","+MessageString.length()+i);
                                com.thlight.wifireceiver.usbSerialPortManager.aIsReceiver[i] = true;
                                //Record the receiver location.
                                aReceiverRecord.add(i);
                            }
                            else
                            {
                                com.thlight.wifireceiver.usbSerialPortManager.aIsReceiver[i] = false;
                                Log.d("usbManager", "usb1:false"+i);
                                usbSerialPortManager.SendCMD(THLApp.SET_KEEP_SETTING, i);
                                //aReceiverRecord.add(i);  //for test
                            }
                        }
                        str1 = "";
                        buf = null;
                    }
                    break;
                case Constants.MSG_NOTIFY_UART:

                    SerialPortFinder abc = new SerialPortFinder();
                    String[] hebe = abc.getAllDevices();

                    for (int i =0; i< hebe.length; i++)
                    {
                        Log.d("5566", "hebe[" + i +"] :" + hebe[i]);
                    }

                    try {
                        spp = new SerialPort(new File(UART_PATH1), BAUD_RATE, 0);
                        spp2 = new SerialPort(new File(UART_PATH2), BAUD_RATE, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mOutputStream=spp.getOutputStream();//(FileOutputStream) spp.getOutputStream();
                    mInputStream=spp.getInputStream();//(FileInputStream) spp.getInputStream();

                    mOutputStream2=spp2.getOutputStream();//(FileOutputStream) spp.getOutputStream();
                    mInputStream2=spp2.getInputStream();//(FileInputStream) spp.getInputStream();

                    try {
                        mOutputStream.write("hebe".getBytes());
                        mOutputStream.write("\n".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    int count = 0;
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            while (true)
                            {
                                try {
                                    mOutputStream.write("help\r\n".getBytes());
                                    Thread.sleep(1000);
                                    //mOutputStream.write("e".getBytes());
                                    Thread.sleep(1000);
                                    //mOutputStream.write("l".getBytes());
                                    Thread.sleep(1000);
                                    //mOutputStream.write("p".getBytes());
                                    Thread.sleep(1000);
                                    //mOutputStream.write("\r\n".getBytes());
                                    Thread.sleep(1000);
                                    mOutputStream2.write("help\r\n".getBytes());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                        }
                    }).start();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            while(true)
                            {
                                int size = 0;

                                byte[] buffer = new byte[128];
                                //if (mInputStream == null) return;
                                try {
                                    size = mInputStream.read(buffer);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.d("5566", "size: " + size );
                                if (size > 0) {
                                    onDataReceived(buffer, size);
                                }

                            }
                        }
                    }).start();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            while(true)
                            {
                                int size = 0;

                                byte[] buffer = new byte[128];
                                //if (mInputStream == null) return;
                                try {
                                    size = mInputStream2.read(buffer);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Log.d("5566", "size2: " + size );
                                if (size > 0) {
                                    onDataReceived(buffer, size);
                                }

                            }
                        }
                    }).start();

                    break;
			}
		}
	};

    SerialPort spp ;
    SerialPort spp2 ;

    InputStream mInputStream;
    OutputStream mOutputStream;

    InputStream mInputStream2;
    OutputStream mOutputStream2;

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
        mp.setVolume(THLApp.volume, THLApp.volume);       
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        //mp.start();
        
        //sp.play(sp_Id,1.0f,1.0f,0,0,0.1f);
        
        //light_time = Integer.valueOf(THLApp.green_light);             //Depend on the gpio.
        
        //mHandler.sendEmptyMessageDelayed(Constants.MSG_SHOW_MAC, 3000);
        /*Delete the log files that create by Banana Pi  for each hour. */
        mHandler.sendEmptyMessageDelayed(Constants.MSG_DELETE_AW_FILE, 1);
        /*解析receiver收到的資料 然後把它存成list, 去調音量*/
        mHandler.sendEmptyMessageDelayed(Constants.MSG_UPLOAD, UPLOAD_FREQUENCY);
        /*對 UART 去做控制, 送資料與接受資料*/
        //mHandler.sendEmptyMessage(Constants.MSG_NOTIFY_UART);

		/*=====================Audio record ==============================*/
        StartRecordAudioVolume();  // 取得背景音量
        /*======================================================================*/

        //TestLED is a thread.
//        TestLED testLED = new TestLED();
//        testLED.start();

        /*
        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> portList = manager.getGpioList();
        if (portList.isEmpty()) {
            Log.d("debug", "No GPIO port available on this device.");
        } else {
            Log.d("debug", "List of available ports: " + portList);
        }
*/
    }

    void onDataReceived(final byte[] buffer, final int size) {

                    Data = (new String(buffer, 0, size));
                Log.d("5566", "Data : " + Data);

    }

    /** ================================================ */
    /*It will go to onResume every time opening APP.*/
   	@Override
   	public void onResume()
	{
		super.onResume();
		usbSerialPortManager.USBInitial();
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
        isGetVoiceRun = false;
        bShowLightStart = false;
        scheduledFuture.cancel(false);        //Cancel show light.
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
    				
    				String OutputString = "";
    		 		
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
                        OutputString = THLApp.SET_INFO_COMMAND + " " +number+" "+ GreenLightHexTime+" "+red_time+"\n";
    				else if(GreenLightHexTime.length() == 3)
                        OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 0"+GreenLightHexTime+" "+red_time+"\n";
    				else if(GreenLightHexTime.length() == 2)
                        OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 00"+GreenLightHexTime+" "+red_time+"\n";
    				else if(GreenLightHexTime.length() == 1)
                        OutputString = THLApp.SET_INFO_COMMAND + " " +number+" 000"+GreenLightHexTime+" "+red_time+"\n";

                    // Send command to Beacon.
    		 		if (usbSerialPortManager.getDeviceSize() >1) {
                        if (!com.thlight.wifireceiver.usbSerialPortManager.aIsReceiver[1]) {
                            usbSerialPortManager.SendCMD(OutputString, 1);
                        }
                    }
                    else
                    {
                        if(!com.thlight.wifireceiver.usbSerialPortManager.aIsReceiver[0])
                        {
                            usbSerialPortManager.SendCMD(OutputString,0);
                        }
                    }
    				
    				tv_green.setBackgroundResource(R.drawable.green_light);
    				tv_red.setBackgroundResource(R.drawable.gray_light);
    				
    				isRed = false;

                    //Stop show light thread at first.
                    bShowLightStart = false;
                    scheduledFuture.cancel(false);
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
    				mp.setVolume(THLApp.volume, THLApp.volume);  // left volume, right volume
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

    public void StartRecordAudioVolume ()
    {
        if (isGetVoiceRun) {
            Log.e("debug", "Already recording.");
            return;
        }

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

        if (mAudioRecord == null) {
            Log.e("debug", "mAudioRecord初始化失败");
        }

        isGetVoiceRun = true;       // Start to record the volume.

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                ArrayList<Double> VolumeList = new ArrayList<Double>();
                int u32RedTime = (Integer.parseInt(THLApp.red_light)) * 1000;   // 紅燈時間 ms
                int u32TakeAverageSize = (u32RedTime * CHANGE_VOLUME_FREQUENCY)/(RECORD_VOLUME_FREQUENCY * REFERENCE_TIMES);

                while (isGetVoiceRun) {

                    try {
                        Thread.sleep(RECORD_VOLUME_FREQUENCY);      // Every 1 second check the background value.
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    //int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);

                    //Only record noise when red light.
                    if (isRed)
                    {
                        int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                        long v = 0;
                        // 将 buffer 内容取出，进行平方和运算
                        for (int i = 0; i < buffer.length; i++) {
                            v += buffer[i] * buffer[i];
                        }
                        //Log.d("terry", "VolumeList.size(): " + VolumeList.size());
                        // 平方和除以数据总长度，得到音量大小。
                        double mean = v / (double) r;
                        double volume = 10 * Math.log10(mean); //取分貝值

                        VolumeList.add(volume);               // Add to a list.
                        //Log.d("terry", "分貝值:" + volume);

                        // 每隔 u32TakeAverageSize 就去調整音量
                        if (VolumeList.size() == u32TakeAverageSize) {

                            ModifyVolumeManner(VolumeList);
                            VolumeList.clear();

                            //Modify the value of volume on the UI on a UI thread.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv_volume.setText(String.valueOf(THLApp.volume));
                                }
                            });
                        }
                    }
                    else
                    {
                        Log.d("debug", "Don't get voice at green light time.");
                        continue;
                    }

                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
                VolumeList.clear();
            }
        }).start();
    }

    // The algorithm of changing volume
    public double ModifyVolumeManner(ArrayList<Double> volumeList)
    {
        dVolumeAverage = 0;

        //Find the extremely value and remove. It's take 10% now.
        for (int i = 0; i < EXTREMELY_VALUE_RANGE * volumeList.size(); i++) {
            Object obj = Collections.max(volumeList);
            volumeList.remove(obj);
            obj = Collections.min(volumeList);
            volumeList.remove(obj);
        }

        for (int i = 0; i < volumeList.size(); i++) {
            dVolumeAverage = dVolumeAverage + volumeList.get(i);
        }
        //Get volume average;
        aAverageVolume.add(dVolumeAverage / volumeList.size());

        if (aAverageVolume.size() == REFERENCE_TIMES)
        {
            dVolumeAverage = 0;
            for (int i =0; i<REFERENCE_TIMES; i++) {
                dVolumeAverage = dVolumeAverage + aAverageVolume.get(i);
                Log.d("terry", "分貝: " + aAverageVolume.get(i));

            }
            dVolumeAverage = dVolumeAverage/REFERENCE_TIMES;

            Log.d("terry", "平均分貝: " + dVolumeAverage);
            //According to the average background volume to modify the volume.
            if (dVolumeAverage > u32BgNoiseLevel2) {
                Log.d("terry", "setVolume: " + fVolumeLevel4);
                mp.setVolume(fVolumeLevel4, fVolumeLevel4);
                THLApp.volume = fVolumeLevel4;
            } else if (dVolumeAverage > u32BgNoiseLevel1 && dVolumeAverage < u32BgNoiseLevel2) {
                Log.d("terry", "setVolume: " + fVolumeLevel3);
                mp.setVolume(fVolumeLevel3, fVolumeLevel3);
                THLApp.volume = fVolumeLevel3;
            } else {
                Log.d("terry", "setVolume: " + fVolumeLevel1);
                mp.setVolume(fVolumeLevel1, fVolumeLevel1);
                THLApp.volume = fVolumeLevel1;
            }

            aAverageVolume.clear();
        }

        return dVolumeAverage;
    }

    /*Get data from beacon to check the GPIO status*/
    public  void CheckGPIO(final int index)
    {

        new Thread(new Runnable() {
            public void run() {

                String sBeaconData = "";
                byte[] buf = new byte[2000];
                int re = usbSerialPortManager.getSerialPortData(buf, index);

                if(re != -1)  // If get data from beacon.
                {
                    for(int j = 0 ; j< re ;j++)
                    {
                        sBeaconData +=(char)buf[j];
                    }

                    //Check GPIO at first.
                    if (sBeaconData.contains("GPIO"))
                    {
                        if (sBeaconData.contains("GPIO6 1"))
                        {
                            RTPause = true;      //Counting red time pause.
                            GTPause = false;     //Starting to count green time.
                            u32GreenTime = 0;

                            u32PreRedTime = u32RedTime;         // Record red time.

                            //It's not first round, and traffic light starts counting.
                            if (u32RedTime != 0)
                                bFirstRound = false;
                            Log.d("hebe", "sBeaconData: "+sBeaconData);
                        }
                        else if (sBeaconData.contains("GPIO6 0")) {
                            GTPause = true;    //Counting green time pause.
                            RTPause = false;    //Starting to count red time.
                            u32RedTime = 0;
                            u32PreGreenTime = u32GreenTime;         // Record green time.
                            Log.d("hebe", "sBeaconData: "+sBeaconData);
                        }
                        else if (sBeaconData.contains("GPIO7 1"))
                        {
                            RTPause = false;
                            u32RedTime = 0;
                            Log.d("hebe", "sBeaconData: "+sBeaconData);
                        }
                        else if (sBeaconData.contains("GPIO7 0")) {
                            RTPause = true;
                        }
                    }

                    Log.d("hebe", "u32PreGreenTime: "+u32PreGreenTime + " u32PreRedTime: " + u32PreRedTime);
                    Log.d("hebe", "u32GreenTime: "+u32GreenTime + " u32RedTime: " + u32RedTime);

                }
                sBeaconData = "";
                buf = null;

            }
        }).start();

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
        final int u32Hex = 16;   //16 進位
        final int u32MajorStart = 55;   // Major number start position.
        final int u32MajorEnd = 59;   // Major number end position.
        final int u32MinorStart = 60;   // Minor number start position.
        final int u32MinorEnd = 64;   // Minor number end position.
        final int u32UuidStart = 18;   // UUID number start position.
        final int u32UuidEnd = 54;   // UUID number end position.
//		Log.d("debug", "==========================");
//		Log.d("debug", MessageString);
//		Log.d("debug", "==========================");
		for(int i = 0 ; i < receivers.length ; i++)
		{
			String data = receivers[i].replace("\r", "");    //for change line
			if(data.length() == Constants.BEACON_DATA_FORMAT_LENGTH1
                    || data.length() == Constants.BEACON_DATA_FORMAT_LENGTH2
                    || data.length() == Constants.BLE_DATA_FORMAT_LENGTH)
			{
				BeaconInfo beaconInfo = new BeaconInfo();
				Date curDate = new Date(System.currentTimeMillis());
				
				beaconInfo.time = formatter.format(curDate);
				beaconInfo.scanned_mac = data.substring(0, Constants.MAC_LENGTH);  //Mac
				try {
					if(data.length() == Constants.BEACON_DATA_FORMAT_LENGTH2)
					{
						beaconInfo.major = String.valueOf(Integer.parseInt(data.substring(u32MajorStart, u32MajorEnd), u32Hex));
						beaconInfo.minor = String.valueOf(Integer.parseInt(data.substring(u32MinorStart, u32MinorEnd), u32Hex));
						beaconInfo.rssi = data.substring(69, 72);
						beaconInfo.uuid = data.substring(u32UuidStart, u32UuidEnd);
					}
					else if(data.length() == Constants.BEACON_DATA_FORMAT_LENGTH1)
					{
                        // 轉成 16進位
						beaconInfo.major = String.valueOf(Integer.parseInt(data.substring(u32MajorStart, u32MajorEnd), u32Hex));
						beaconInfo.minor = String.valueOf(Integer.parseInt(data.substring(u32MinorStart, u32MinorEnd), u32Hex));
						beaconInfo.rssi = data.substring(68, 71);
						beaconInfo.uuid = data.substring(u32UuidStart, u32UuidEnd);
					}
					else if(data.length() == Constants.BLE_DATA_FORMAT_LENGTH){        //BLE DATA
						beaconInfo.rssi = data.substring(81, 84);
						beaconInfo.uuid = data.substring(19, 78);
					}			
				} catch (Exception e) {
					// TODO: handle exception
					Log.d("debug", "major or minor is not integer");
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

				if(!beaconInfo.uuid.equals(THLApp.RECEIVE_UUID))
				{
                    Log.d("debug", "UUID is not match.");
					continue;
				}
                else
                {
                    Log.d("debug", "UUID : " + beaconInfo.uuid);
                }

				//Log.d("debug", THLApp.uuid + "," + beaconInfo.uuid + ",pass");
				/********************************************************************************/
                /* Check RSSI, and update beaconList*/
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
					else // 如果beaconList 不是空的, 先判斷是否有重複, 有重複就更新原本資料, 沒有就加
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
                else
                {
                    Log.d("debug", "beaconList" + i + "RSSI  is empty.");
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
        return !str.contains("command not found");
    }
	/***************************************/
	private boolean isMac(String val) {  
        String trueMacAddress = "([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}";  
        // 这是真正的MAV地址；正则表达式；  
        return val.matches(trueMacAddress);
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
