package com.intel.developer.extend;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.intel.fangpei.task.handler.Extender;

public class wyextend4 extends Extender {
	public wyextend4(){
		System.out.println("nothing to do!");
	}
	//֧�ִ�������ǲ���ֻ����String���ͣ�
	public wyextend4(String s){
		System.out.println(s);
	}
	public wyextend4(String s,String s2){
		System.out.println(s+"~"+s2);
	}
	public void commitTask(){
		 
			Date nowTime = new Date(System.currentTimeMillis());
			SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String start = sdFormatter.format(nowTime);
			System.out.println("4 start at : "+start);

			for(int i=0;i<5;i++){				
				try {
					System.out.println("4:"+i);
					Thread.sleep(1000);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("4 end");
			FileWriter fw;
			try {
				fw = new FileWriter("D:/wyextend4.txt");
				fw.write("end");
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
}
