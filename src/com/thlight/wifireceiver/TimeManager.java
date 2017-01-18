package com.thlight.wifireceiver;

import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

public class TimeManager {

	/***********************************************************************/
	public void changeSystemTime(String year,String month,String day,String hour,String minute,String second){
	    try {
	        Process process = Runtime.getRuntime().exec("su");
	        DataOutputStream os = new DataOutputStream(process.getOutputStream());
	        String command = "date -s "+year+month+day+"."+hour+minute+second+"\n";
	        Log.e("command",command);
	        os.writeBytes(command);
	        os.flush();
	        os.writeBytes("exit\n");
	        os.flush();
	        process.waitFor();
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
