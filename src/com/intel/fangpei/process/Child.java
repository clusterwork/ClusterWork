package com.intel.fangpei.process;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang.ArrayUtils;

import com.intel.fangpei.BasicMessage.BasicMessage;
import com.intel.fangpei.BasicMessage.packet;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.NIONodeHandler;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.task.ExtendTask;
import com.intel.fangpei.task.TaskRunner;
import com.intel.fangpei.task.TaskRunner.SplitId;
import com.intel.fangpei.util.ConfManager;

public class Child {
	static MonitorLog ml = null;
	static TaskRunner tr = null;
	static SplitId childid = null;
	static String taskid = "";
	static String jvmId = "";
	static ArrayList<Future<Boolean>> splitFuture = new ArrayList<Future<Boolean>>();
    private static ExecutorService executorService; 
	public static void main(String[] args) throws Throwable {
		// start init
		//ml = new MonitorLog("/tmp/log/childlog");
		ml = new MonitorLog("c:/childlog");
		ml.log("a new child is in construct...");
		ConfManager.addResource(null);
		String serverip = args[0];
		int port = Integer.parseInt(args[1]);
		// start node handler to send & recv data
		NIONodeHandler node = new NIONodeHandler(serverip, port);
		Thread demon = new Thread(node);
		demon.setDaemon(true);
		demon.start();
		jvmId = args[2];
		// send login packet
		packet one = new packet(BasicMessage.NODE, BasicMessage.OP_LOGIN,
				jvmId.getBytes());
		node.addSendPacket(one);
		// get head info here
		packet head = null;
		while ((head = node.getReceivePacket()) == null)
			;
		String[] heads = new String(head.getArgs()).split(" ");
		System.out.println("args is : " + new String(head.getArgs()));
		taskid = heads[0];
		jvmId = heads[1];
		SplitExecutor executor = new SplitExecutor(Integer.parseInt(jvmId));
		// use RPC to registe this child to RPCServer
		RpcClient rpcClient = RpcClient.getInstance();
		Object[] params = new Object[] { Integer.parseInt(taskid),
				Integer.parseInt(jvmId)};
//		rpcClient.execute("TaskChildHandler.registeChild", params);
		// final String logLocation = args[3];
		// int jvmIdInt = Integer.parseInt(jvmId);
		// String cwd = System.getenv().get(TaskRunner.TASK_WORK_DIR);
		// if (cwd == null) {
		// throw new IOException("Environment variable " +
		// TaskRunner.TASK_WORK_DIR + " is not set");
		// }

		//shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					if (childid != null) {
						RpcClient rpcClient = RpcClient.getInstance();
						Object[] params = new Object[] { Integer
								.parseInt(jvmId) };
						rpcClient.execute("TaskChildHandler.setChildKilled",
								params);
						// add shutdown hook context;
					}
				} catch (Throwable throwable) {
				}
			}
		});
		Thread t = new Thread() {
			public void run() {
				// every so often wake up and syncLogs so that we can track
				// logs of the currently running task
				while (true) {
					try {
						Thread.sleep(5000);
						if (childid != null) {
							// demond task here;
						}
					} catch (InterruptedException ie) {
					}
				}
			}
		};
		t.setName("Thread for Logs");
		t.setDaemon(true);
		t.start();
		ml.log("child started!");
		// wait time for task prepare
		long millis = 100;
		packet inprocessTaskPacket = null;
		while (true) {
			if ((inprocessTaskPacket = node.getReceivePacket()) == null) {
				Thread.sleep(millis);
				continue;
			} else {
				System.out.println("child " + jvmId + " recevied:"
						+ new String(inprocessTaskPacket.getBuffer().array()));
				byte[] taskArgsbytes = inprocessTaskPacket.getArgs();
				if (taskArgsbytes == null) {
					System.out.println("fack!!!-----------error");
				}
				String taskArgsString = new String(taskArgsbytes);
				String[] taskArgs = taskArgsString.trim().split(" ");
				String taskname = taskArgs[0];
				ml.log("receive a new task:" + taskname);
				/*
				 * end the process signal.
				 */
				if (taskname.equals("break")) {
					//maybe there contains some unfinished work.flush them.
					executor.flush();
					// all complete,send exit request.
					ml.log("all work of the JVM:" + jvmId + " have complete.");
					one = new packet(BasicMessage.NODE, BasicMessage.OP_QUIT,
							jvmId.getBytes());
					node.addSendPacket(one);
					node.flush();
					//Thread.sleep(1000);
					while (true) {
						//while (node.getReceivePacket() != null)
						//	;
						// use rpc to registe the child complete percent.

						params = new Object[] { Integer.parseInt(jvmId) };
						rpcClient.execute("TaskChildHandler.completesplit",
								params);
						//wait for server cancel key
						Thread.sleep(1000);
						System.out.println("[Child]Exit");
						System.exit(0);
					}

				}
				//normal split work to submit
				executor.submit(taskArgs);

			}
		}

	}
}
