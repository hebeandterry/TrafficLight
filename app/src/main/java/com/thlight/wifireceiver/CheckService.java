package com.thlight.wifireceiver;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class CheckService extends Service {

	final String Package_ict	= "com.thlight.traffic_light";
	
	final int MSG_CHECK_ACTIVITY = 1000;
	final int REMOVE_MSG_CHECK_ACTIVITY = 1001;

	Handler mHandler= new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			Bundle bundle = new Bundle();
			Message message = null;
			switch(msg.what)
			{
				case MSG_CHECK_ACTIVITY:
					if(!isActiviting(Package_ict))
					{
						Intent intent= getPackageManager().getLaunchIntentForPackage(Package_ict);
		    			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(intent);						
					}				
					Log.d("debug", "Check Activity");
					mHandler.sendEmptyMessageDelayed(MSG_CHECK_ACTIVITY,10000); //Every 10 seconds check.
					break;
				case REMOVE_MSG_CHECK_ACTIVITY:
					mHandler.removeMessages(MSG_CHECK_ACTIVITY);
					break;	
			}
		}
	};
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Messenger messenger = new Messenger(mHandler);

		return  messenger.getBinder(); 
	}

	/** ========================================================== */
	/** onCreate will be called once if the service doesn't active. */
	@Override
	public void onCreate()
	{
		super.onCreate();
		mHandler.removeMessages(MSG_CHECK_ACTIVITY);
		mHandler.sendEmptyMessageDelayed(MSG_CHECK_ACTIVITY,10000);
		Log.d("debug", "service onCreate");
	}
	/** ========================================================== */
	@Override
	 public void onStart(Intent intent, int startId) {
	 }
	/** ========================================================== */
	 @Override
	 public void onDestroy() {
		 mHandler.removeMessages(MSG_CHECK_ACTIVITY);
		 Log.d("debug", "service onDestroy");
	 }
	
	 /** ========================================================== */
	boolean isActiviting(String packageName)
	{
		ActivityManager activityManager			= (ActivityManager)getSystemService(ACTIVITY_SERVICE);
	    List<RunningAppProcessInfo> procInfos	= activityManager.getRunningAppProcesses();

	    if(null != procInfos)
	    {
    	    for(RunningAppProcessInfo procInfo : procInfos)
    	    {
				//Log.d("check", "procInfo.processName: " + procInfo.processName + " procInfo.importance: " +procInfo.importance);

    	        if(procInfo.processName.equals(packageName) && (procInfo.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND
    	           || procInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND))
    	        {
    	            return true;
    	        }
    	    }
	    }

	    return false;
	}
	

}
