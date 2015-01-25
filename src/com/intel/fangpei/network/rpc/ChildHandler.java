package com.intel.fangpei.network.rpc;

import java.nio.channels.SelectionKey;

import com.intel.fangpei.terminalmanager.AdminManager;
import com.intel.fangpei.util.Line;

public class ChildHandler {
	private ServerRunningResource stm;
	public ChildHandler(){
		stm  = ServerRunningResource.getServerTaskMonitorInstance();
	}
	
	public boolean registeChild(int hostid,int taskid, int childid){
		stm.newChild(hostid,taskid, childid);
		return true;
	}
	public Object[] getChilds(){
		return stm.getChilds();
	}
	public String getChildSummary(){
		return stm.getChildSummary();
	}
	public boolean addsplit(int childid,String classname,String[] args){
		stm.addLoad(childid, classname, args);
		return true;
	}
	public boolean startsplit(int childid){
		stm.startone(childid);
		return true;
	}
	public boolean splitPercent(int childid,String classname,Double percent){
		stm.processone(childid,classname,percent);
		return true;
	}
	public boolean completesplit(int childid){
		
		stm.completeone(childid);
		return true;
	}
	public String getChildInfo(int childid){
		return stm.getChildInfo(childid);
	}
//	public double getChildPercent(int child_id){
//		return stm.getChildPercent(child_id);
//	}

	public boolean setChildKilled(int child_id){
		return stm.setChildKilled(child_id);
	}
	public boolean iskilled(int child_id){
		return stm.iskilled(child_id);
	}

}
