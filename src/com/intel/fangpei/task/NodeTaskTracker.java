package com.intel.fangpei.task;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.ali.fangpei.service.wrapWork;
import com.intel.fangpei.BasicMessage.BasicMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.NIOServerHandler;
import com.intel.fangpei.network.SelectionKeyManager;
import com.intel.fangpei.network.PacketLine.segment;
import com.intel.fangpei.task.TaskRunner.SplitWork;
import com.intel.fangpei.terminalmanager.AdminManager;
import com.intel.fangpei.terminalmanager.ClientManager;
import com.intel.fangpei.util.ServerUtil;
import com.intel.fangpei.util.SystemUtil;
import com.intel.fangpei.util.TimeCounter;

public class NodeTaskTracker {
private TaskNotifyServer server = null;
private NIOServerHandler serverhandler = null;
private int taskid = 10000;
private boolean isRunning = false;
/***
 * bug:monitorlog д���ļ���ͻ����
 */
private MonitorLog ml = null;
private ArrayList<TaskRunner> runners = new ArrayList<TaskRunner>();
NodeTaskManager ntm  = null;//added
public NodeTaskTracker(MonitorLog ml){
	serverhandler = new NIOServerHandler(4399,ml);
	this.ml = ml;
	server = new TaskNotifyServer();
	server.startServer();
	//wait for tasknotifyserver to start...
	try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	ntm = new NodeTaskManager();//added
}
public int nextTaskID(){
	taskid++;
	return taskid;
}
public void report(String s){
	ml.log(s);
}
public void addNewTaskMonitor(TaskRunner tr){
	runners.add(tr);
	tr.setBoss(this);
	new Thread(tr).start();
}
//added
public synchronized void addNewTaskMonitorWithPriority(TaskRunner tr,Integer priority){
	tr.setBoss(this);
	ntm.registerTaskRunner(tr, priority);
	if(!ntm.isStarted()){
			System.out.println("NTM start!!!");
			new Thread(ntm).start();
		ntm.setStarted(true);
	}	
}
public void send(int jvmId,packet p){
	server.send(jvmId, p);
}
public void send(int jvmId,SplitWork cr){
	server.send(jvmId, cr);
}
public void send(int jvmId,wrapWork ww){
	server.send(jvmId, ww);
}
public boolean isRegisted(int jvmid){
	return server.isRegistedJvm(jvmid);
}
public boolean isRegisted(Set<Integer> set){
	return server.isRegisted(set);
}
public class NIOTrackerProcess implements Runnable{
	HashMap<Integer,SelectionKey> registedJvm = new HashMap<Integer,SelectionKey>();
	NIOServerHandler nioserverhandler = null;
	public NIOTrackerProcess(NIOServerHandler nioserverhandler) {
		this.nioserverhandler = nioserverhandler;
	}
	@Override
	public void run() {
		while (true) {
			segment se = nioserverhandler.getNewSegement();
			if (se == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			SelectionKey key = se.key;
			packet p = se.p;
			int jvmid = 0;
			if(p.getCommand() == BasicMessage.OP_LOGIN){
				jvmid = Integer.parseInt(p.getArgs().toStringUtf8());
				registedJvm.put(jvmid,key);
				System.out.println("[NIOTrackerProcess]new child connected!id is: "+jvmid);
				continue;
			}else if(p.getCommand() == BasicMessage.OP_QUIT){
				jvmid = Integer.parseInt(p.getArgs().toStringUtf8());
				registedJvm.remove(jvmid);
				
				//serverhandler.pushWriteSegement(key, new packet(BasicMessage.NODE, BasicMessage.OK));
				serverhandler.flush();
				key.cancel();
				System.out.println("[NodeTaskTracker]child id: "+jvmid+", have notify to complete!");
			}
		}

	}

}
	public class TaskNotifyServer{
		NIOTrackerProcess nioprocess =  new NIOTrackerProcess(serverhandler);
		//extend selectionkeymanager to enble key manager instance.
		HashMap<Integer,SelectionKey> registedJvm = null;
		public boolean isRegistedJvm(int jvmid) {
			return registedJvm.containsKey(jvmid);		
		}
		public boolean isRegisted(Set<Integer> set){
			if(registedJvm.keySet().containsAll(set)){
				return true;
			}
			else{
				return false;
			}
		}
		public void send(int jvmId, packet p) {
			System.out.println("[NodeTaskTracker]start send:"+p+" to "+jvmId);
			SelectionKey key = registedJvm.get(jvmId);
			if(key == null){
				ml.error("we cann't find the Service JVM ,"
						+ ",maybe the JVM has been destroyed,"
						+ "please do check the ServiceDemo!");
			}
			serverhandler.pushWriteSegement(key, p);
			
		}
		public synchronized void send(int jvmId,SplitWork cr){
			packet p = cr.toTransfer();
			send(jvmId,p);
		}
		public synchronized void send(int jvmId,wrapWork ww){
			packet p = ww.toTransfer();
			send(jvmId,p);
		}
		public void startServer(){
			new Thread(serverhandler).start();	
			new Thread(nioprocess).start();
			registedJvm = nioprocess.registedJvm;
			isRunning = true;
			ml.logWithThreadName("TaskTracker is running...");
}
	}
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return isRunning;
	}
	public void killNotifyServer(){
		isRunning = false;
	}


}
