/** ======================================================================== */
package com.thlight.wifireceiver;
import java.io.File;
import java.util.regex.Pattern;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


//import com.THLight.BLE.USBeacon.Writer.Simple.AccountInfo;

/** ======================================================================== */
public class THLApp extends Application
{
	public static final boolean DEMO = false;
	public static final String REF_RSSI = "C1";   /*IOS calculate the RSSI by REF_RSSI.*/
    public static String red_light = "";
    public static String green_light = "";
    public static String number = "";
    public static float volume = 0.0f;
	public static boolean bEastWest = false;         //< Decide for the direction.
    public static boolean bSouthNorth = false;
    
    public static String deviceName = "";

    public static THLApp App = null;     
    
    public static String STORE_PATH	 = Environment.getExternalStorageDirectory().toString()+ "/Traffic Light/";
	public static String SET_INFO_COMMAND = "set_beacon_info E2C56DB5-DFFB-48D2-B060-D0F5A71096E1";
    public static String RECEIVE_UUID = "436DFAB4-03AF-4F10-A039-4503BB94BD56";
	public static String SET_KEEP_SETTING = "set_keep_setting 0 \n";          //Set writing Major Minor to the ROM.
    public static String GET_KEEP_SETTING = "get_keep_setting \n";
     
	/** ========================================================== */
	public THLApp()
	{
		super();
		App = this;
	}
	
	/** ========================================================== */
	@Override
	public void onCreate()
	{
		super.onCreate();
		File file= new File(STORE_PATH);
		if(!file.exists())
		{
			if(!file.mkdirs())
			{
				Toast.makeText(this, "Create folder("+ STORE_PATH+ ") failed.", Toast.LENGTH_SHORT).show();
			}
		}
		loadSettings();

        startService(new Intent(this, CheckService.class));  //Start APP when APP is closed accidentally.
	}

	/** ========================================================== */
	@Override
	public void onTerminate()
	{		
		super.onTerminate();
	}
	
	 /** ========================================================== */
	public static void loadSettings()
	{
		SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(App);
		
		/** */		
		red_light  		 = sp.getString("red_light", "10");
		green_light  	     = sp.getString("green_light", "30");
		volume   		 = sp.getFloat("volume",  1.0f);
		number			 = sp.getString("number",  "39");             //Default device number.
		//IP   		 = sp.getString("IP", "61.216.93.208");

	}
		
	/** ========================================================== */
	public static void saveSettings()
	{
		SharedPreferences sp			= PreferenceManager.getDefaultSharedPreferences(App);
    	SharedPreferences.Editor edit	= sp.edit();

    	edit.putString("red_light", red_light);
    	edit.putString("green_light", green_light);
    	edit.putFloat("volume", volume);
    	edit.putString("number", number);

    	edit.commit();

	}

	/** ========================================================== */
	public static boolean isNumeric(String str) {
	    Pattern pattern = Pattern.compile("[0-9.-]+");
	    return pattern.matcher(str).matches();
	}
	/** ========================================================== */
	public static boolean isNumerAndEnglish(String str) {
	    Pattern pattern = Pattern.compile("[a-eA-e0-9]+");
	    return pattern.matcher(str).matches();
	}
	
}

/** ======================================================================== */

