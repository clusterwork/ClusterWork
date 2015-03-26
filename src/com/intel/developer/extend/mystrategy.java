package com.intel.developer.extend;

import com.intel.fangpei.process.ChildStrategy;

public class mystrategy extends ChildStrategy{
	public boolean canDoNextWork(){
		try {
			System.out.println("sleep 20 s in mystrategy");
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

}
