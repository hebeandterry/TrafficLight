package com.thlight.wifireceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/*****************************/
class BeaconInfo implements Cloneable 
{
	String time = "";
	String scanned_mac = "";
	String uuid = "";
	String major = "";
	String minor = "";
	String rssi = "";
	
	String isOK = "";
	int count = 0;
	ArrayList<Double> rssiList = new ArrayList<Double>();
	int thread = -100;
	//private int samplecount = 3;
	public Double avgRssi(int samplecount)
	{
		if(rssiList.size()<=samplecount)
		{
			return Double.valueOf(rssi);
		}
		else 
		{
			ArrayList<Double> tempList = new ArrayList<Double>();
			
			for(int i = rssiList.size()-samplecount;i<rssiList.size();i++)
			{
				tempList.add(rssiList.get(i));
			}
			
			Double sum = 0.0;
			
			Collections.sort(tempList, new Comparator<Double>() 
			{
				@Override
				public int compare(Double o1, Double o2) {
					// TODO Auto-generated method stub
					return o1.compareTo(o2);
				}
		    });
	        //Collections.sort(rssiList, new Comparator<Double>()); 
	        
			for(int i=0;i<tempList.size();i++)
			{
				sum = sum + tempList.get(i);
			}
			
			rssi = String.valueOf(Double.valueOf(String.format("%.2f", (sum/(tempList.size())))));

			if(Double.valueOf(String.format("%.2f", (sum/(tempList.size()))))> thread)
			{
				isOK = "OK";
			}
			else
			{
				isOK = "fail";
			}
			
			return Double.valueOf(rssi);
		}

	}
	
	public void add(Double rssi)
	{
		rssiList.add(rssi);
	}
	
	public BeaconInfo clone()
	{
		return this;
	}
}
