package com.intel.fangpei.terminal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.ali.fangpei.service.wrapWork;
import com.intel.fangpei.BasicMessage.BasicMessage;
import com.intel.fangpei.BasicMessage.HeartBeatMessage;
import com.intel.fangpei.BasicMessage.ServiceMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.SystemInfoCollector.SysInfo;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.HeartBeatThread;
import com.intel.fangpei.network.NIONodeHandler;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.process.ChildStrategy;
import com.intel.fangpei.process.MyChildStrategy;
import com.intel.fangpei.process.ProcessFactory;
import com.intel.fangpei.process.ProcessManager;
import com.intel.fangpei.task.TaskRunner;
import com.intel.fangpei.task.TaskStrategy;
import com.intel.fangpei.task.NodeTaskTracker;
import com.intel.fangpei.util.ClientUtil;
import com.intel.fangpei.util.ConfManager;
import com.intel.fangpei.util.Line;
import com.intel.fangpei.util.ReflectFactory;
import com.intel.fangpei.util.SystemUtil;
/**
 * start a Node proccess.
 */
public class Node extends Client {
	MonitorLog ml = null;
	SysInfo si = null;
	String serverip = "";
	int port = 0;
	int id = 0;
	NodeTaskTracker tracker = null;
	int serviceDemoJvmid = -1;
	boolean serviceDemoIsRunning = false;
	//add heartbeat thread
	private HeartBeatThread hbThread = null;

	Node(String serverip, int port) {
		try {
			ml = new MonitorLog();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.serverip = serverip;
		this.port = port;
		this.connect = new NIONodeHandler(serverip, port);
		tracker = new NodeTaskTracker(ml);// need port to pass in***
		try {
			this.hbThread = new HeartBeatThread(connect,InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		si = SysInfo.GetSysHandler();
	}

	@Override
	public void run() {
		if (!tracker.isRunning()) {
			ml.log("tracker is not running ,return");
			// no need to work any further;
			return;
		}
		try {
			
			new Thread(connect).start();
			//heart beat thread start
			new Thread(hbThread).start();
			packet p = null;
			while (true) {
				if (connect.waitReadNext()) {
					p = connect.getReceivePacket();
					if (!Do(p)) {
						System.exit(0);
					}
				}
			}
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) {
		ConfManager.addResource(null);
		String ip = ConfManager.getConf("selectsocket.server.ip");
		if (ip == null) {
			System.out.println("not config ip ...exit...");
			System.exit(0);
		}
		int port = ConfManager.getInt("selectsocket.server.port", 1234);
		for (int i = 0; i < 1; i++) {
			Node c = new Node(ip, port);
			new Thread(c).start();
			packet one = PacketProtocolImpl.CreatePacket(BasicMessage.NODE, BasicMessage.OP_LOGIN);
			c.connect.addSendPacket(one);
		}
	}

	public boolean Do(packet message) throws IOException {
		String[] args = null;
		if(message == null){
			System.out.println("[Node]message is none!");
			return false;
		}
		/*
		 * we need to unpack the received packet and check the command !
		 */
		if (message.getArgs() != null) {
			args = message.getArgs().toStringUtf8().split(" ");
		}
		int opt = message.getCommand();
		switch (opt) {
		case BasicMessage.OP_LOGIN:
			ml.log("messge from server ,[login] the node,id is:"+args[0]);
			id = Integer.parseInt(args[0]);
			RpcClient rpcclient = RpcClient.getInstance();
			Object[] params = new Object[]{id,InetAddress.getLocalHost().getHostAddress()};
			rpcclient.execute("HostHandler.runningHost", params);
			return true;
		case BasicMessage.OP_CLOSE:
			ml.log("messge from server ,[close] the node");
			packet one = PacketProtocolImpl.CreatePacket(BasicMessage.NODE, BasicMessage.OP_QUIT);
			connect.addSendPacket(one);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		case BasicMessage.OP_EXEC:
			
			if (args.length < 3) {
				ml.log("no args to exec,return");
				return true;
			}
			if (args.length == 3) {
				ml.log("exec 1");
				extendTask("com.intel.developer.extend." + args[1],args[2]);
			} else {
				ml.log("exec 2");
				extendTask("com.intel.developer.extend." + args[1],
						(String[]) ArrayUtils.subarray(args, 2, args.length));
			}
			ml.log("complete execute!" + System.currentTimeMillis());
			return true;
		case ServiceMessage.THREAD:
		case ServiceMessage.SERVICE:
			ml.log("Thread|Service Request with args:"
					+ (args == null ? "no args!"
							: message.getArgs()));
			extendThreadWork("java", null);
			return true;
			/*
			 * haven't complete now, wait next version.
			 */
			// case BasicMessage.OP_lOAD_DISK:
			// ml.log("load data into disk,command parameter is :"+args == null
			// ? "no args!" : new String(message
			// .getArgs()));
			// if(!loadData_Disk(args)){
			// ml.error("load data fail!");
			// return true;
			// }
			// return true;
		case BasicMessage.OP_SYSINFO:
			try {
				si.Refresh();
				Map m = si.GetSysInfoMap();
				// System.out.println("NetWork_FQDN:"+m.get("NetWork_FQDN"));
				// System.out.println("NetWork_IP:"+m.get("NetWork_IP"));
			} catch (Exception e) {
				ml.log(e.getMessage());
				return false;
			}
			packet p = PacketProtocolImpl.CreatePacket(BasicMessage.NODE, BasicMessage.OP_SYSINFO,
					new String(si.GetSysInfoBytes()));
			connect.addSendPacket(p);
			return true;
		case BasicMessage.OP_SH:
			ml.log("excute a new single node command");
			//add node heart beat call back operation
		default:
			return true;
		}
	}

	/**
	 * added
	 * @param classname
	 * @return
	 */
	public boolean extendTask(String childStrategyName,String[] classname,int priority){
		ChildStrategy childStrategy = getChildStrategy(childStrategyName);
		TaskRunner tr = getTaskRunner(childStrategy,classname);
		tracker.addNewTaskMonitorWithPriority(tr, priority);
		return true;
	}
	private ChildStrategy getChildStrategy(String childStrategyName){
		if(childStrategyName==null){
			return new ChildStrategy();
		}
		ReflectFactory factory = ReflectFactory.getInstance();
		Object obj;
		try {
			obj = factory.getClass(childStrategyName).newInstance();
			if(obj instanceof ChildStrategy){
				ml.log("return childstrategy: "+childStrategyName);
				return (ChildStrategy)obj;
			}
			else{
				
				ml.log("cannot find childStrategy: "+childStrategyName);
			 
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			System.out.println("cannot find childStrategy: "+childStrategyName+", return default childstrategy");
			ml.log("return default childstrategy");
			//return new ChildStrategy();
		}
		return new ChildStrategy();
	}
	/**
	 * added 
	 * @param classname
	 * @return
	 */
	private TaskRunner getTaskRunner(ChildStrategy childStrategy,String[] classname){
		TaskRunner tr = new TaskRunner();
		TaskStrategy strate = null;
		strate = new TaskStrategy();
		strate.addStrategy(childStrategy,classname);
		tr.setTaskStrategy(strate);
		return tr;
	}
	private boolean extendTask(String  classname,String childStrate){
		TaskRunner tr = new TaskRunner();
		TaskStrategy strate = null;
		strate = new TaskStrategy();
//		strate.addStrategy(tr.getDefaultStrategy(),new String[]{classname});
		strate.addStrategy(childStrate,new String[]{classname});

		tr.setTaskStrategy(strate);
		tracker.addNewTaskMonitor(tr);
		/*
		 * example add strategy Random r = new Random(); while(true){ try {
		 * Thread.sleep(50*r.nextInt(100)); } catch (InterruptedException e) {
		 * break; } strate.addStrategy(tr.getDefaultStrategy(),new
		 * String[]{classname}); }
		 */
		return true;
	}

	private boolean extendTask(String classname, String[] args) {
		TaskRunner tr = new TaskRunner();
		TaskStrategy strate = null;
		strate = new TaskStrategy();
		Line<String, String[]> splits = new Line<String, String[]>();
		splits.addNode(classname, args);
		strate.addStrategy(tr.getDefaultStrategy(), splits);
		tr.setTaskStrategy(strate);
		tracker.addNewTaskMonitor(tr);
		return true;
	}

	public boolean extendService(String classname, String[] args) {
		return false;

	}

	public boolean extendThreadWork(String classname, String[] args) {
		if (!serviceDemoIsRunning) {
			/***
			 * for test ,only support win now!
			 */
			Thread t = new Thread() {
				public void run() {
					if (SystemUtil.operationType().startsWith("Win")) {
						serviceDemoJvmid = ProcessFactory
								.buildNewProcessWithProcessid(
										"java",
										"-cp",
										"../cluster.jar",
										"-Djava.ext.dirs=../tools/lib",
										"com.intel.fangpei.process.ServiceDemo",
										"127.0.0.1", "4399");

					} else {
						String lib = SystemUtil.buildSysPath();
						serviceDemoJvmid = ProcessFactory
								.buildNewProcessWithProcessid(
										"java",
										"-cp",
										lib,
										"com.intel.fangpei.process.ServiceDemo",
										"127.0.0.1", "4399");
					}
					ProcessManager.start(serviceDemoJvmid);
				}
			};
			t.setDaemon(true);
			t.start();
			while (!tracker.isRegisted(serviceDemoJvmid))
				;
			ml.log("service Demo have registed on the tracker!");
			serviceDemoIsRunning = true;
		}
		tracker.send(serviceDemoJvmid, new wrapWork(classname, false));
		return true;

	}

	/**
	 * added for test
	 */
	public Node(){
		ConfManager.addResource(null);
		try {
			ml = new MonitorLog();
		} catch (Exception e) {
			e.printStackTrace();
		}	
		tracker = new NodeTaskTracker(ml);//need port to pass in***
	}

}
