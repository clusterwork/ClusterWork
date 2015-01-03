package com.intel.fangpei.process;

import java.util.HashMap;
import java.util.Map;

import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.task.ChildWork;
import com.intel.fangpei.task.NodeTaskTracker;
import com.intel.fangpei.task.TaskRunner.SplitWork;
import com.intel.fangpei.util.ClientUtil;
import com.intel.fangpei.util.Line;
import com.intel.fangpei.util.Line.segment;

public class ChildStrategy extends Thread{
	private StartegyRunner runner = null;
	Line<String,String[]> splits = new Line<String,String[]>();
	//clean env:close the jvm 
	private String lastwork="break";
	private boolean iskilled = false;
	private boolean retake = false;
	private int runningflag = 0;//0 not run 1 runing 2 interrupt 3 finish
	public ChildStrategy(){
	}
	public ChildStrategy(boolean retake){
		this.retake = retake;
	}
	public ChildStrategy(ChildStrategy childStrategy){
		this.splits = childStrategy.splits;
		this.iskilled = false;
		this.retake = false;
		this.runner = null;
	}

	public synchronized void addLoad(String classname,String[] args){
//		System.out.println("***"+map.get(map.keySet().iterator().next())[0]);
		splits.addNode(classname, args);
	}
	public void addLoads(Line<String, String[]> splits) {
		this.splits.addAll(splits);
		
	}
	public Line<String,String[]> getLoads(){
		return splits;
	}
	public String getLastWork(){
		return lastwork;
	}
	public  boolean canDoNextWork(){
		System.out.println("childStrategy  return true");
		return true;
	}
	public void startStrategyRunner(NodeTaskTracker boss, ChildWork taskManager){
		if(runningflag != 0){
			System.out.println("[ChildStrategy]double run error!");
			return;
		}
		runner = new StartegyRunner(this,boss,taskManager);
		//runner.setDaemon(true);
		runner.start();
	}
	/**
	 *  闁告艾濂旂粩瀛樼▔閻氣暡m濞戞搩鍘惧▓鎴炵鐠囨彃顬為柡鍕靛灠閹線鎳楅懞銉モ挃閻炴稑鑻崹搴㈢▔閵堝棙钂嬬紒鎯у皡缁憋拷
	 *  1.闁活枌鍔嶉崺娑㈡嚊椤忓嫮鏆板☉鏂款樈濞堟垹鎲撮崟顐㈢仧闁哄嫷鍨伴幆渚�礂娴ｇ瓔鍟呭☉鎾愁儎缁斿瓨绋夐鍐╁床闁告柡鍓濇晶鐣屾偘閿燂拷	 *  2.鐟滅増鎸告晶鐖卾m闁哄嫷鍨伴幆浣虹矚濞差亝锛�
	 *
	 */
	public class StartegyRunner extends Thread{
		private NodeTaskTracker boss = null;
		private RpcClient rpc = null;
		private ChildWork childwork = null;
		private ChildStrategy childStrate = null;
		public StartegyRunner(ChildStrategy childStrate,NodeTaskTracker boss, ChildWork taskManager){
			this.childStrate = childStrate;
			this.boss = boss;
			this.childwork = taskManager;
			this.rpc = RpcClient.getInstance();
		}
		public void run(){
			runningflag = 1;
			System.out.println("[ChildStartegy]boss is running:"+boss.isRunning());
			System.out.println("[ChildStartegy]has no work to assign?:"+childwork.noSplitAssign());
			while(boss.isRunning()&&!childwork.noSplitAssign()&&!iskilled){	
				if(childStrate.canDoNextWork()){
					if(childwork.nextWork()){
//						try {
//							Thread.sleep(3000);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
						// check RPC here,whether the child has been killed.
						// if killed,removejvm.
						segment s = childwork.getSplit();
						if (s == null) {
							System.out
									.println("ummm......we got null ....I don't know");
							continue;
						}
						System.out.println("[ChildStrategy]get segment:" + s.v);
						SplitWork splitwork = (SplitWork) s.v;
						boss.send(childwork.jvmId, splitwork);
						boss.report("[ChildStrategy]assign new task, thename is:"
								+ splitwork.taskname);
						//registe start of the child split
						rpc.execute("TaskChildHandler.startsplit", new Object[]{childwork.jvmId});
						System.out.println("[ChildStartegy]assign new task");
					}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//remove the childstrategy from taskrunner , taskrunner will monitor it
			runningflag = 3;
		}
	}
	public int getstate(){
		return runningflag;
	}
	public void kill() {
		runningflag = 2;
		iskilled = true;
		
	}
	//added20140428
	public ChildStrategy clone(){
		ChildStrategy child = new ChildStrategy();
		child.splits = this.splits;
		return child;
	}

}
