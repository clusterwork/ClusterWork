package com.intel.fangpei.process;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import com.intel.fangpei.network.rpc.RpcClient;

public class MyChildStrategy2 extends ChildStrategy {
	private int flag = 0;
	private int mark = 1;

	/**
	 * added20140428
	 * @return 
	 */
	public ChildStrategy clone(){
		MyChildStrategy2 child = new MyChildStrategy2();
		child.splits = this.splits;
		child.init();
		return child;
	}
	public void init(){
		this.flag = 0;
		this.mark = 1;
	}

	public  boolean canDoNextWork(){
		
		if(flag ==0){
			flag = 1;
			System.out.println("first strategy access always return true");
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
//					if(tmp.equals(Integer.toString(mark))){
//						System.out.println(name+" will return true");
//						mark++;
//						System.out.println("mychildStrategy2  return true");
//						System.out.println("file content is: "+tmp);
//						return true;
//						//return false;
//					}
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
			return false;
		}
		
	}
	public static void clean(){
		FileWriter fw;
		try {
			fw = new FileWriter("D:/myextend1.txt");
			fw.write(" ");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
