package com.intel.fangpei.resource.metrix;

import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.resource.metrix.Metrix;
import com.intel.fangpei.resource.metrix.TaskMetrix;

public class HostMetrix extends Metrix{
	Date start = null; 
	Date down = null;
	String ip = "0.0.0.0";
	CopyOnWriteArrayList<ChildMetrix> childs = new CopyOnWriteArrayList<ChildMetrix>();
	CopyOnWriteArrayList<TaskMetrix> tasks = new CopyOnWriteArrayList<TaskMetrix>();
	public HostMetrix(int hostid,String ip){
		id = hostid;
		this.ip = ip;
	}
	public String getIp(){
		return ip;
	}
	public void pointTo(Metrix metrix){
		if ( metrix.getClass() == TaskMetrix.class ){
			tasks.add((TaskMetrix) metrix);
		}
		else if ( metrix.getClass() == ChildMetrix.class ){
			childs.add((ChildMetrix) metrix);
		}
	}
	public void removePoint(Metrix metrix){

	}
}