package com.intel.fangpei.network.rpc;

import java.util.List;
import java.util.Map;

import com.intel.fangpei.terminalmanager.AdminManager;

public class TaskHandler {
	private ServerRunningResource stm;
	public TaskHandler(){
		stm  = ServerRunningResource.getServerTaskMonitorInstance();
	}
//	
	public boolean registeTask(int task_id){
		stm.newTask(task_id);
		return true;
	}
	public boolean isTaskComplete(int task_id){
		
		return stm.isComplete(task_id);
	}
	public Object[] getTasks(){
		return stm.getTasks();
	}
	public Map<Integer,Double> getTaskInfo(int task_id){
		return stm.getTaskInfo(task_id);
	}
	//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	public int  NotifyNodeTimeout(String key){
		System.out.println("node:"+key+" is timeouted!");
		return 0;
	}
}
