package com.intel.fangpei.network.rpc;

import java.util.Map;

import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.resource.metrix.MCODE;
import com.intel.fangpei.resource.metrix.MetrixCircle;
import com.intel.fangpei.resource.metrix.TaskMetrix;

public class ServerRunningResource {
	static MetrixCircle  circle= new MetrixCircle();
	private static ServerRunningResource SRR;
	
	public static ServerRunningResource getServerTaskMonitorInstance() {
		if (SRR == null) {
			SRR = new ServerRunningResource();
		}
		return SRR;
	}
		
		private TaskMetrix taskhandler = null;
//		private ChildMetrix childhandler = null;
//		private HostMetrix hosthandler = null;
		
		//Child RPC METHOD
		public void initChild(int taskid,int childid){
			ChildMetrix child = new ChildMetrix(childid);
			circle.newChild(child, MCODE.OTHER);
			circle.connect(circle.findChild(childid), circle.findTask(taskid));
		}
		public void addLoad(int childid,String classname,String[] args){
			ChildMetrix child= circle.findChild(childid);
			child.addLoad(classname, args);
		}
		public void startone(int childid){
			ChildMetrix child = circle.findChild(childid);
			if (child!=null)
				child.startone();
			else
				System.out.println("*******************************8");
		}
		public void completeone(int childid) {
			ChildMetrix child = circle.findChild(childid);
			if (child!=null)
				child.completeone();
		}
		public boolean setChildKilled(int childid) {
			ChildMetrix child= circle.findChild(childid);
			if (child != null){
				child.state = MCODE.INTERRUPT;
				return true;
			}
			return false;
		}
		public boolean iskilled(int childid){
			ChildMetrix child= circle.findChild(childid);
			if (child != null){
				return child.state == MCODE.INTERRUPT;
			}
			return false;
		}
		public String getChildInfo(int childid){
			ChildMetrix child = circle.findChild(childid);
			return "state:"+child.state+"\npercent:"+child.percent()+"\nworks:"+child.works();
		}
		//task RPC METHOD
		public boolean newTask(int taskid) {
			System.out.println("[AdminManager] new task"+taskid);
			taskhandler = new TaskMetrix(taskid);
			circle.newTask(taskhandler,MCODE.OTHER);
			return true;
		}
		public boolean isComplete(int taskid) {
			
            return circle.findTask(taskid).state == MCODE.FINISH;
		}

//		public double getChildPercent(int childid) {
//			
//			return circle.findChild(childid).percent();
//
//		}
		//only finish task getter
		public Object[] getTasks(){
			return circle.retrivetasks();
			
		}
		public Map<Integer, Double> getTaskInfo(int taskid){

			TaskMetrix task = circle.findTask(taskid);
			return task.taskchildinfo();
			
		}
	}
