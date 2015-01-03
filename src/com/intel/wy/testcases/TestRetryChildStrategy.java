package com.intel.wy.testcases;

import java.io.FileWriter;
import java.io.IOException;

import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.process.MyChildStrategy2;
import com.intel.fangpei.process.ProcessFactory;
import com.intel.fangpei.task.NodeTaskTracker;
import com.intel.fangpei.task.TaskRunner;
import com.intel.fangpei.task.TaskStrategy;
import com.intel.fangpei.terminal.Node;
import com.intel.fangpei.util.ConfManager;

public class TestRetryChildStrategy {

	/**把MyChildStrategy2的第54行"return true"改成"return false"，运行次测试用例，将重复打印两次1:1到1:5
	 * change <class>MyChilStrategy2<class> 54 line from "return true" to "return false",
	 * run this demo will print twice from 1:1 to 1:5
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		clean();
		ConfManager.addResource(null);
		NodeTaskTracker tracker = new NodeTaskTracker(new MonitorLog());	
		TaskRunner tr1 = createTaskRunner(new String[]{"com.intel.developer.extend.myextend1"});
		tracker.addNewTaskMonitorWithPriority(tr1, 0);
//		Node n = new Node();
//		n.extendTask("com.intel.fangpei.process.MyChildStrategy2",new String[]{"com.intel.developer.extend.myextend1"}, 0);
		tryKillLastProcess();
	}
	public static void clean(){
		FileWriter fw;
		try {
			fw = new FileWriter("D:/myextend1.txt");
			fw.write(" ");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static TaskRunner createTaskRunner(String[] s){
		TaskRunner tr = new TaskRunner();
		TaskStrategy strate = null;
		strate = new TaskStrategy();
		//strate.addStrategy(tr.getDefaultStrategy(),s);
		strate.addStrategy(new MyChildStrategy2(),s);
		tr.setTaskStrategy(strate);
		return tr;
		
		
	}
	public static void tryKillLastProcess(){
		try {
			Thread.sleep(3000);
			RpcClient client = RpcClient.getInstance();
			int id = ProcessFactory.getProcessNum();
			Object [] params = {id};
			Thread.sleep(1000);
			client.execute("TaskChildHandler.setChildKilled", params);
			String function = "TaskChildHandler.iskilled";
			boolean result = (Boolean) client.execute(function,
					params);
			System.out.println("--------------------------------------");
			System.out.println("iskilled is "+result+"");
			System.out.println("--------------------------------------");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
