package com.intel.fangpei.process;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;

import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.task.ExtendTask;

public class SplitExecutor {
	int numTasksToExecute = -1; // -1 signifies "no limit"
	int numTasksExecuted = 0;
	MonitorLog ml = null;
	int jvmId = 0;
	private ArrayList<SplitWork> works = new ArrayList<SplitWork>();
	RpcClient rpcClient = RpcClient.getInstance();
	public SplitExecutor(int jvmId,MonitorLog ml){
		this.jvmId = jvmId;
		this.ml = ml;
	}
	public class SplitWork extends Thread{
		Thread workthread = null;
		ExtendTask task = null;
		public SplitWork(String[] taskArgs){
			String taskname = taskArgs[0];
			if (taskArgs.length > 1) {
				String[] otherArgs = (String[]) ArrayUtils.subarray(
						taskArgs, 1, taskArgs.length);
				task = new ExtendTask(jvmId,rpcClient,ml, taskname, otherArgs);
				System.out.println("extend task with args");
			} else {
				task = new ExtendTask(jvmId,rpcClient,ml, taskname);
			}
//			workthread = new Thread(task);
//			workthread.setName("child jvm work thread");
		}
		public void run(){
			new Thread(task).start();
			while(true){
				if(task.setCompletePercent()){
					break;
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		public void flush(){
			try {
				this.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void submit(String[] taskArgs){
		SplitWork work = new SplitWork(taskArgs);
		works.add(work);
		work.start();
	}
	//uncomplete
	public void flush() {
		Iterator<SplitWork> i = works.iterator();
		while(i.hasNext()){
			i.next().flush();
		}
		
	}
}
