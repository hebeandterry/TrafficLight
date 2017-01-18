package com.thlight.wifireceiver;

import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

public class TestLED extends Thread {

    private String TAG = "Test-LED";

    TestLED() {
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            Runtime command = Runtime.getRuntime();
            Process proc;
            DataOutputStream opt;
            proc = command.exec("su");
            opt = new DataOutputStream(proc.getOutputStream());
            Log.d(TAG, "LED Testing start.");
            while (true) {
                LEDRun(opt);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(TAG, "LED Testing run."+e.toString());
        }
    }

    private void LEDRun(DataOutputStream opt) {
        try {
            opt.writeBytes("echo 1 > /sys/class/gpio_sw/PA1/data\n");  
            opt.writeBytes("echo 1 > /sys/class/gpio_sw/PA16/data\n");  
//          Log.d(TAG, "LED ON.");
            Thread.sleep(100);
//            opt.writeBytes("echo 0 > /sys/class/gpio_sw/PA1/data\n");   
//            opt.writeBytes("echo 0 > /sys/class/gpio_sw/PA16/data\n");  
////          Log.d(TAG, "LED OFF.");
//            Thread.sleep(100);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "LED Testing start."+e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "LED Testing start."+e.toString());
            e.printStackTrace();
        }
    }
    private void LEDRun2(DataOutputStream opt) {
        try {
            opt.writeBytes("echo 1 > /sys/class/gpio_sw/PA16/data\n");        
//          Log.d(TAG, "LED ON.");
            Thread.sleep(40);
            opt.writeBytes("echo 0 > /sys/class/gpio_sw/PA16/data\n");        
//          Log.d(TAG, "LED OFF.");
            Thread.sleep(10);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "LED Testing start."+e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
        	Log.d(TAG, "LED Testing start."+e.toString());
            e.printStackTrace();
        }
    }

}