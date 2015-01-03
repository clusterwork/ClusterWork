package com.intel.fangpei.for_test_code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class test2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<Future<?>> splitFuture = new ArrayList<Future<?>>();
	    ExecutorService executorService = Executors.newCachedThreadPool();; 
		Thread t = new Thread() {
			public void run() {
				// every so often wake up and syncLogs so that we can track
				// logs of the currently running task
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ie) {
					}
				
			}
		};
		splitFuture.add(executorService  
                .submit(t));
		ArrayList<Object> a = new ArrayList<Object>();
		Object o1 = (Object)"1";
		Object o2 = (Object)"2";
		Object o3 = (Object)"3";
		a.add(o1);
		a.add(o2);
		a.add(o3);
		a.remove(1);
		System.out.println(a.get(1));		
		try {
			for(InetAddress addr:InetAddress.getAllByName(InetAddress.getLocalHost().getHostName()))
			    System.out.println(addr);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	System.out.println("woca");
	System.out.print("~~"+new File("d:\\").isDirectory());
	HashMap<String, String> map = new HashMap<String, String>();
	map.put("CPU", "core count:4core type:E5-2660X 2.0HZ");
	byte[] b = serialize(map);
	HashMap<String, String> c = deserialize(b);
	System.out.println(c.get("CPU"));
	}
	public static byte[] serialize(HashMap<String, String> hashMap){ 
        try { 
        ByteArrayOutputStream mem_out = new ByteArrayOutputStream(); 
            ObjectOutputStream out = new ObjectOutputStream(mem_out); 
 
            out.writeObject(hashMap); 
 
            out.close(); 
           mem_out.close(); 
 
           byte[] bytes =  mem_out.toByteArray(); 
           return bytes; 
        } catch (IOException e) { 
            return null; 
        } 
    }
    public static HashMap<String, String> deserialize(byte[] bytes){ 
        try { 
            ByteArrayInputStream mem_in = new ByteArrayInputStream(bytes); 
            ObjectInputStream in = new ObjectInputStream(mem_in); 
 
            HashMap<String, String> hashMap = (HashMap<String, String>)in.readObject(); 
 
             in.close(); 
             mem_in.close(); 
 
             return hashMap; 
        } catch (StreamCorruptedException e) { 
            return null; 
        } catch (ClassNotFoundException e) { 
            return null; 
        }   catch (IOException e) { 
            return null; 
        } 
     }
} 
