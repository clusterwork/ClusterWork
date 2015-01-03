package com.intel.fangpei.process;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Set;

public class MyChildStrategy extends ChildStrategy {
	private int flag = 0;

	/**
	 * 绫诲悕+鈥�txt鈥濆緱鍒扮殑鏂囦欢瀛樻斁浠诲姟杩愯缁撴灉锛屽鏋滃唴瀹规槸end琛ㄧず鍙互缁х画鍋氫笅涓�釜浠诲姟
	 */
	public  boolean canDoNextWork(){
		if(flag ==0){
			flag = 1;
			return true;
		}else{
//			Set set = splits.keySet();
//			Iterator it = set.iterator();
//			if(it.hasNext()){
//				String s = (String) it.next();
//				String name = "D:/"+s.substring(s.lastIndexOf(".")+1,s.length())+".txt";
//				//System.out.println("file name is : " +name);
//				char[] charbf = new char [100];
//				try {
//					FileReader fr = new FileReader(name);
//					fr.read(charbf);
//					String tmp = new String(charbf).trim();
//					if(tmp.equals("end")){
//						System.out.println(name+" will return true");
//						return true;
//					}
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
			return false;
		}
		
	}
	
}
