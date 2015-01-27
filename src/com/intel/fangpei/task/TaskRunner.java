package com.intel.fangpei.task;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.intel.fangpei.BasicMessage.BasicMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.process.ChildStrategy;
import com.intel.fangpei.process.MyChildStrategy2;
import com.intel.fangpei.process.ProcessFactory;
import com.intel.fangpei.process.ProcessManager;
import com.intel.fangpei.terminalmanager.AdminManager;
import com.intel.fangpei.network.rpc.ServerRunningResource;
import com.intel.fangpei.util.ConfManager;
import com.intel.fangpei.util.Line;
import com.intel.fangpei.util.Line.segment;
import com.intel.fangpei.util.SystemUtil;

public class TaskRunner implements Runnable {
	public static String TASK_WORK_DIR = null;
	private int taskid = -1;
	private SplitWork child = null;
	private NodeTaskTracker boss = null;
	// private Map<SplitId,Integer> ChildIdTojvmId = null;
	// private Map<Integer,SplitId> jvmIdToChildId = null;
	private Map<JvmRunner, String> jvmToid = null;
	private Map<Integer, ChildWork> idToChildWork = null;
	private Map<Integer, ChildStrategy> idToStrategy = null;
	private Map<Integer, ChildStrategy> innormalKilled = null;
	private int runningChildsCount = 0;
	private int maxRunningChilds = 0;
	private int completeChilds = 0;
	private ChildStrategy defaultChildStrate = null;
	private TaskStrategy taskstrategy = null;
	private Object maplock = null;
	private boolean started = false;// added
	private boolean finished = false;// added20140428
	private RpcClient rpc = null;

	public TaskRunner() {
		maplock = new Object();
		rpc = RpcClient.getInstance();
		// ChildIdTojvmId = new HashMap<SplitId,Integer>();
		// jvmIdToChildId = new HashMap<Integer,SplitId>();
		jvmToid = new HashMap<JvmRunner, String>();
		idToChildWork = new HashMap<Integer, ChildWork>();
		idToStrategy = new HashMap<Integer, ChildStrategy>();
		innormalKilled = new HashMap<Integer, ChildStrategy>();
	}

	public void setTaskStrategy(TaskStrategy starte) {
		this.taskstrategy = starte;
	}

	public void setBoss(NodeTaskTracker tracker) {
		this.boss = tracker;
		this.taskid = tracker.nextTaskID();
	}

	public void report(String s) {
		boss.report(s);
	}

	public ChildStrategy getDefaultStrategy() {
		return new ChildStrategy();
	}

	/*
	 * 3.add JvmTask to the TaskRunner
	 */
	private ChildWork expandNewJvm(TaskEnv env,int hostid,ChildStrategy childStrategy) {
		Object[] params = new Object[]{hostid};
		String ip = (String)rpc.execute("HostHandler.getHost", params);
		JvmRunner jvm = new JvmRunner(ip, 4399);
		// jvm.setDaemon(true);
		jvm.start();

		while (!jvm.isStarted()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ChildWork jvmtask = null;
		synchronized (maplock) {
			jvmToid.put(jvm, "" + jvm.getpid());
			idToStrategy.put(jvm.getpid(), childStrategy);//changed20140428
			System.out.println("JvmTask jvmToPid:" + jvm.getpid());
			if (env != null) {
				jvmtask = new ChildWork(jvm.getpid(), env);
			} else {
				jvmtask = new ChildWork(jvm.getpid());
			}
			jvmtask.SetTaskRunner(this);
			idToChildWork.put(jvm.getpid(), jvmtask);
			maplock.notifyAll();
		}
		return jvmtask;
	}

	private synchronized ChildWork expandNewJvm(int hostid,ChildStrategy childStrategy) {
		return expandNewJvm(null,hostid,childStrategy);
	}

	public void removeJvm(int jvmId) {
		// remove jvmtoid jvm ~~here
		synchronized (maplock) {
			idToStrategy.get(jvmId).kill();
			idToStrategy.remove(jvmId);
			idToChildWork.remove(jvmId);
			maplock.notifyAll();
		}
	}

	public class SplitWork {
		private TaskEnv env = null;
		public String taskname = null;
		String[] args = null;

		public SplitWork(String taskname) {
			this.taskname = taskname;

		}

		public void setEnv(TaskEnv env) {
			this.env = env;
		}

		public void setArgs(String... args) {
			this.args = args;
		}

		/*
		 * one task name and args
		 */
		public packet toTransfer() {
			StringBuilder sb = new StringBuilder();
			sb.append(taskname);
			sb.append(" ");
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					sb.append(args[i]);
					sb.append(" ");
				}
			}
			return PacketProtocolImpl.CreatePacket(BasicMessage.NODE, BasicMessage.OP_MESSAGE, sb
					.toString());
		}
	}

	/*
	 * start child process and wait;
	 */
	public class JvmRunner extends Thread {
		private int processid = -1;
		private boolean started = false;

		public JvmRunner(String ip, int port) {

		}

		public boolean isStarted() {
			return started;
		}

		public int getpid() {
			return processid;
		}

		public void run() {
			if (SystemUtil.operationType().startsWith("Win")) {
				processid = ProcessFactory.buildNewProcessWithProcessid("java",
						"-cp", "../cluster.jar",
						"-Djava.ext.dirs=../tools/lib",
						"com.intel.fangpei.process.Child", "127.0.0.1", "4399");
			} else {
				String userlib = ConfManager.getConf("node.task.lib.path");
				String lib = SystemUtil.buildSysPath();
				if (userlib == null) {
					processid = ProcessFactory.buildNewProcessWithProcessid(
							"java", "-cp", lib,
							"com.intel.fangpei.process.Child", "127.0.0.1",
							"4399");
				} else {
					System.out.println("ClusterMonitor User extended path is:"
							+ userlib);
					processid = ProcessFactory.buildNewProcessWithProcessid(
							"java", "-cp", lib, "-Djava.ext.dirs=" + userlib,
							"com.intel.fangpei.process.Child", "127.0.0.1",
							"4399");
				}
				// ProcessManager.get(processid).setAllEnv(System.getenv());
				// ProcessManager.get(processid).setWorkDir("/root/fangpei");
			}
			started = true;
			ProcessManager.start(processid);
		}
	}

	/*
	 * child conf metrix;
	 */
	public class SplitId {
		public int id = 0;

		public SplitId() {
			this.id = new Random().nextInt(1000);
		}

		public int maxMemoryToUse = 0;
	}

	public class TaskEnv {
		public String env = "";
	}

	public static void setupWorkDir(SplitId childid, File file) {

	}

	@Override
	public void run() {
		if (taskstrategy == null) {
			System.out.println("*no strategy to this TaskRunner*");
			return;
		}
		// starte.addStrategy(this.getDefaultStrategy(), new
		// String[]{"com.intel.developer.extend.myextend"});
		boss.report("start taskRunner!");
		//registe taskid
		rpc.execute("TaskHandler.registeTask", new Object[]{taskid});
		registeAllChildStrategy();
		waitForChildUp();
		// send jvmid and taskid to every child
		sendAllChildHead();
		
		startAllStrategys();
		boss.report("complete TaskRunner init!");

		while (true) {
			if ((Boolean) rpc.execute("TaskHandler.isTaskComplete", new Object[]{taskid})) {
				System.out.println("task runner exit");
				finished = true;
				break;
			}
			if (taskstrategy.hasNewStrategy()) {
				registeAllChildStrategy();
				// send these new child head here
				waitForChildUp();
				
				sendAllChildHead();//added20140428
				
				startAllStrategys();
			}
			synchronized (maplock) {
				Object[] ids = idToChildWork.keySet().toArray();
				int len = ids.length;
				for (int i = 0; i < len; i++) {
					ChildWork childwork = idToChildWork.get(ids[i]);
					// if is been killed ,removeJVM
					if ((Boolean) RpcClient.getInstance().execute(
							"ChildHandler.iskilled",
							new Object[] { (Integer) ids[i] })) {
						innormalKilled.put(childwork.jvmId, idToStrategy.get(childwork.jvmId));//added20140428
						//add rpc remove childid^^^^^^^^^^^^^^^^.
						removeJvm(childwork.jvmId);
						continue;
					}
					// if no work assgin,remove JVM
					if (childwork.noSplitAssign()) {
						removeJvm(childwork.jvmId);
					}
				}
				maplock.notifyAll();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private void startAllStrategys() {
		//System.out.println("[TaskRunner]start one strategy1");
		//HashMap<ChildStrategy, Boolean> childs = taskstrategy.ChildStrategys();
		Object[] childScanner = idToStrategy.keySet().toArray();
		ChildStrategy child = null;
		
		int childnum = childScanner.length;
		for (int i = 0; i < childnum; i++) {
			//System.out.println("[TaskRunner]start one strategy");
			int id = (Integer)childScanner[i];
			child = idToStrategy.get(id);
			if (child.getstate() == 0) {
				child.startStrategyRunner(boss, idToChildWork.get(id));
			}
		}
	}

	private void sendAllChildHead() {
		Object[] ids = idToChildWork.keySet().toArray();
		int len = ids.length;
		for (int i = 0; i < len; i++) {
			ChildWork childwork = idToChildWork.get(ids[i]);
			boss.send(childwork.jvmId, PacketProtocolImpl.CreatePacket(BasicMessage.SERVER,
					BasicMessage.TASKINFO, taskid + " " + childwork.jvmId));
		}
	}

	public void registeAllChildStrategy() {
		HashMap<ChildStrategy, Boolean> childs = taskstrategy.ChildStrategys();
		Object[] childScanner = childs.keySet().toArray();
		ChildStrategy child = null;
		
		int childnum = childScanner.length;
		for (int i = 0; i < childnum; i++) {
			child = (ChildStrategy) childScanner[i];
			if (childs.get(child).equals(true)) {
				continue;
			}
			Object[] hosts = (Object[])rpc.execute("HostHandler.getHosts", new Object[]{});
			System.out.println("[TaskRunner]get one hosts:"+hosts[0]);
			//#TODO
			int hostid = (Integer)hosts[0];
			ChildWork childwork = expandNewJvm(hostid,child);
			int jvmid = childwork.getId();
			rpc.execute("ChildHandler.registeChild", new Object[]{hostid,taskid,jvmid});
			Line<String, String[]> loads = child.getLoads();
			while (loads.hasNext()) {
				segment load = loads.popNode();
				SplitWork tmpchild = new SplitWork((String) load.k);
				// if(loads.get(load)!=null){
				// System.out.println("(*)"+loads.get(load)[0]+":"+loads.get(load)[1]);
				// }
				tmpchild.setArgs((String[]) load.v);
				childwork.assignNewSplit(new SplitId(), tmpchild);
				rpc.execute("ChildHandler.addsplit",new Object[]{jvmid,(String) load.k,(String[]) load.v});
			}
			// get last work of the JVM
			childwork.assignNewSplit(new SplitId(),
					new SplitWork(child.getLastWork()));
			rpc.execute("ChildHandler.addsplit",new Object[]{jvmid,child.getLastWork(),null});	
			
//			child.startStrategyRunner(boss, jvmtask);
			childs.put(child, true);
		}
		taskstrategy.flagRunning();
	}

	public void extendNewStrategy(ChildStrategy strategy, String[] classname) {
		taskstrategy.addStrategy(strategy, classname);
	}

	public void extendNewStrategy(ChildStrategy strategy,
			Line<String,String[]> splits) {
		taskstrategy.addStrategy(strategy, splits);
	}

	private void waitForChildUp() {
		while (true) {
			if (boss.isRegisted(idToChildWork.keySet())) {
				return;
			}
			try {
				// we need to know when the child will started ALL
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// added
	public boolean isStarted() {
		return started;
	}

	// added
	public void setStarted(boolean started) {
		this.started = started;
	}

	// added
	public int getJvmNum() {
		return taskstrategy.getJvmNum();
	}

	public int getTaskId() {
		return taskid;
	}

	// added20140428
	public Object[] getChildIds() {
		Set<Integer> keys = idToChildWork.keySet();
		Object[] ids = keys.toArray();
		return ids;
	}

	// added20140428
	public ChildStrategy getChildStrategy(int id) {
		return idToStrategy.get(id);
	}

	// added20140428
	public boolean isFinished() {
		return finished;
	}

	// added20140428
	public TaskRunner clone() {
		if (finished) {
			TaskRunner tr = new TaskRunner();
			tr.setTaskStrategy(new TaskStrategy());
			tr.boss = this.boss;
			tr.taskid = this.taskid;
			return tr;
		} else {
			return this;
		}
	}
	//added20140428
	public  ArrayList<ChildStrategy> getInnormalKilled(){
		ArrayList<ChildStrategy> list = new ArrayList<ChildStrategy>();
		
		Set<Integer> keySet = innormalKilled.keySet();
		for(Integer i:keySet){
			list.add(innormalKilled.get(i));
		}
		return list;
	}
}
