package com.intel.fangpei.network.rpc;

import java.util.Map;

import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.resource.metrix.HostMetrix;
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
	
		//Host RPC METHOD
		public void newHost(int hostid,String ip){
			HostMetrix host = new HostMetrix(hostid,ip);
			circle.newHost(host, MCODE.OTHER);
		}
		public String getHost(int hostid){
			return circle.findHost(hostid).getIp();
		}
		//task RPC METHOD
		public boolean newTask(int taskid) {
			System.out.println("[AdminManager] new task"+taskid);
			taskhandler = new TaskMetrix(taskid);
			circle.newTask(taskhandler,MCODE.OTHER);
			return true;
		}
		//Child RPC METHOD
		public void newChild(int hostid,int taskid,int childid){
			ChildMetrix child = new ChildMetrix(childid);
			circle.newChild(child, MCODE.OTHER);
			circle.connect(circle.findHost(hostid), circle.findChild(childid));
			circle.connect(circle.findChild(childid), circle.findTask(taskid));
			circle.connect(circle.findTask(taskid), circle.findHost(hostid));
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
				System.out.println("*******************************not found child*******");
		}
		public void processone(int childid,String classname,Double percent){
			ChildMetrix child = circle.findChild(childid);
			if (child!=null)
				child.setProcess(classname, percent);
			else
				System.out.println("*******************************not found child*******");
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
			return child.toString();
		}
		public boolean isComplete(int taskid) {
			
            return circle.findTask(taskid).state == MCODE.FINISH;
		}
		public Object[] getHosts(){
			return circle.hosts();
		}
		public Object[] getChilds(){
			return circle.childs();
		}
		public String getHostSummary(){
			return circle.hostSummary();
		}
		public String getChildSummary(){
			return circle.childSummary();
		}
		public String getTaskSummary(){
			return circle.taskSummary();
		}
		public Object[] getTasks(){
			return circle.retrivetasks();
		}
		public Map<Integer, String> getTaskInfo(int taskid){

			TaskMetrix task = circle.findTask(taskid);
			return task.taskchildinfo();
			
		}
	}
