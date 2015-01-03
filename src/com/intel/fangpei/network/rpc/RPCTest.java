package com.intel.fangpei.network.rpc;

public class RPCTest {
	public static void main(String[] args){
		RpcClient c1 = RpcClient.getInstance();
		Object[] params = new Object[] 
		        { new Integer(1),new Integer(2) }; 
		
		
		 params = new Object[] 
		        { new Integer(1) }; 
		c1.execute("TaskHandler.registeTask", params);
		params = new Object[] 
		        { new Integer(2) }; 
		c1.execute("TaskHandler.registeTask", params);
		params = new Object[] 
		        { "hello" }; 		
		c1.execute("TaskHandler.NotifyNodeTimeout", params);
		params = new Object[] 
		        { new Integer(2),new Integer(1), new Double(1), }; 
		c1.execute("TaskChildHandler.registeChild", params);
		params = new Object[] 
		        { new Integer(2),new Integer(2), new Double(0.1), }; 
		c1.execute("TaskChildHandler.registeChild", params);
		params = new Object[] 
		        { new Integer(2) }; 
		Integer result = (Integer) c1.execute("TaskHandler.getTaskCompleteNum", params); 
		System.out.println("++++++++++complete++++"+result);
	}
}
