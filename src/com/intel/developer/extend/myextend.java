package com.intel.developer.extend;
import com.intel.fangpei.task.handler.Extender;

public class myextend extends Extender {
	public myextend(){
		System.out.println("nothing to do!");
	}
	public myextend(String s){
		System.out.println(s);
	}
	public myextend(String s,String s2){
		System.out.println(s+"~"+s2);
	}
	public void commitSplit(){
		System.out.println("work over!");
		super.setCompletePercent(0.90);
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
