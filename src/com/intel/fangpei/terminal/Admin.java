package com.intel.fangpei.terminal;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.intel.fangpei.BasicMessage.BasicMessage;
import com.intel.fangpei.BasicMessage.ServiceMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.network.NIOAdminHandler;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.util.CommandPhraser;
import com.intel.fangpei.util.ConfManager;
/**
 * start a Admin Proccess.
 * @author fangpei
 *
 */
public class Admin extends Client {
	public static boolean debug = true;
	/*
	 * buffer to buffer the packet of command packet components: [Client Type]
	 * byte [version] int [arg size] int [command] byte [args...] byte[]
	 */
	ByteBuffer buffer = ByteBuffer.allocate(1024);
	String serverip = "";
	int port = 0;
	
	

	public Admin(String serverip, int port) {
		this.serverip = serverip;
		this.port = port;
		this.connect = new NIOAdminHandler(serverip, port);
		
	}

	@Override
	public void run() {
		try {
			
			new Thread(connect).start();
			
			while (true) {
				byte[] b = new byte[1024];
				System.out.print("--->]:");
				System.in.read(b);
				DoCommand(new String(b).trim());
				b = new byte[1024];
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// maybe some command need admin to receive server's request.
	private String DoCommand(String command) throws Exception {
		String[] s = null;
		packet one = null;
		byte COMMAND = CommandPhraser.GetUserInputCommand(command);
		if(COMMAND == BasicMessage.OP_HELP){
			printHelp();
		}
		if(COMMAND == BasicMessage.HOSTS){
			getHosts();
		}
		if(COMMAND == BasicMessage.CHILDS){
			getChilds();
		}
		//add tasks command
		if(COMMAND == BasicMessage.TASKS){
			getTasks();
		}
		if(COMMAND == BasicMessage.TASKINFO){
			int task_id = Integer.parseInt(command.substring(command.indexOf(" ")+1,
					command.length()));
			getTaskInfo(task_id);
		}		
		if(COMMAND == BasicMessage.HOSTINFO){
			int host_id = Integer.parseInt(command.substring(command.indexOf(" ")+1,
					command.length()));
			getHostInfo(host_id);
		}
		if(COMMAND == BasicMessage.CHILDINFO){
			int child_id = Integer.parseInt(command.substring(command.indexOf(" ")+1,
					command.length()));
			getChildInfo(child_id);
		}
		if (COMMAND == BasicMessage.OP_EXEC) {
			try{
			one = PacketProtocolImpl.CreatePacket(BasicMessage.ADMIN, COMMAND,
					command.substring(command.indexOf(" "),
							command.length()));
			}catch(IndexOutOfBoundsException e){
				System.out.println("exec [classname]");
				return "";
			}
		}
		if(COMMAND == BasicMessage.OP_SYSINFO){
		one = PacketProtocolImpl.CreatePacket(BasicMessage.ADMIN,COMMAND);
		}
		if (COMMAND == BasicMessage.OP_CLOSE) {
		one = PacketProtocolImpl.CreatePacket(BasicMessage.ADMIN, COMMAND);
		}
		if (COMMAND == BasicMessage.OP_QUIT) {
		one = PacketProtocolImpl.CreatePacket(BasicMessage.ADMIN, COMMAND);
		}
		if (COMMAND == BasicMessage.OP_SH) {
			one = PacketProtocolImpl.CreatePacket(BasicMessage.ADMIN, COMMAND,
					command.substring(command.indexOf(" "),
							command.length()));
		}
		if (COMMAND == ServiceMessage.THREAD||COMMAND == ServiceMessage.SERVICE) {
			try{
			one = PacketProtocolImpl.CreatePacket(BasicMessage.ADMIN, COMMAND,
					command.substring(command.indexOf(" "),
							command.length()));
			}catch(Exception e){
				System.out.println("thread|service args");
				return "";
			}
		}
		if(one == null)
			return "";
		else
		connect.addSendPacket(one);
		return "";// need to return the server's response!
	}

//	private String formString(String[] s) {
//		StringBuilder sb = new StringBuilder();
//		int len = s.length;
//		for (int i = 0; i < len; i++) {
//			sb.append(" " + s[i]);
//		}
//		return sb.toString();
//	}
	public static void printHelp(){
		System.out.println("the command line Admin Usage:");
		System.out.println("exec          execute a class which extend Extender");
		System.out.println("progress      [not complete command,soon...]");
		System.out.println("close         close the cluster's nodes demon");
		System.out.println("quit          close the admin process");
		System.out.println("sysinfo       get the cluster's system info");
		System.out.println("service       execute a class which extend Extender in a Thread");
		System.out.println("tasks         show tasks in the cluster");
		System.out.println("taskinfo      show task info");
	}
	public static void getHosts(){
		RpcClient rpcclient = RpcClient.getInstance();
		Object[] params = new Object[]{};
		String hosts = (String)rpcclient.execute("HostHandler.getHostSummary", params);
		System.out.println(hosts);
	}
	public static void getChilds(){
		RpcClient rpcclient = RpcClient.getInstance();
		Object[] params = new Object[]{};
		String childs = (String) rpcclient.execute("ChildHandler.getChildSummary", params);
		System.out.println(childs);
	}
	public static void getTasks(){
		RpcClient rpcclient = RpcClient.getInstance();
		Object[] params = new Object[]{};
		String tasks = (String) rpcclient.execute("TaskHandler.getTaskSummary", params);
		System.out.println(tasks);
		
	}
	public static void getTaskInfo(int task_id){
		RpcClient rpcclient = RpcClient.getInstance();
		Object[] params = new Object[]{task_id};
		Map<Integer,String> task_info = (Map<Integer,String>)rpcclient.execute("TaskHandler.getTaskInfo", params);
		Iterator<Entry<Integer,String>> itr = task_info.entrySet().iterator();
		System.out.println("Task Info of  "+task_id+":\n");
		while(itr.hasNext()){
			Entry<Integer, String> tmp = itr.next();
			int id = tmp.getKey();
			String metix = tmp.getValue();
			System.out.println("Child: "+id+", metrix: "+metix);
		}
		System.out.println("------------------------------");
	}
	public static void getHostInfo(int host_id){
		RpcClient rpcclient = RpcClient.getInstance();
		Object[] params = new Object[]{host_id};
		String childinfo = (String) rpcclient.execute("HostHandler.getHost", params);
		System.out.println(childinfo);
	}
	public static void getChildInfo(int child_id){
		RpcClient rpcclient = RpcClient.getInstance();
		Object[] params = new Object[]{child_id};
		String childinfo = (String) rpcclient.execute("ChildHandler.getChildInfo", params);
		System.out.println(childinfo);
	}
	public static void main(String[] args) {
		ConfManager.addResource(null);
		String ip = ConfManager.getConf("selectsocket.server.ip");
		if(ip == null){
			System.out.println("not config ip ...exit...");
			System.exit(0);
		}
		int port = ConfManager.getInt("selectsocket.server.port", 1234);
		for (int i = 0; i < 1; i++) {
			Admin c = new Admin(ip, port);
			new Thread(c).start();
		}
	}
}

