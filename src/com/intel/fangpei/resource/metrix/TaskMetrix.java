package com.intel.fangpei.resource.metrix;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.resource.metrix.HostMetrix;
import com.intel.fangpei.resource.metrix.Metrix;

public class TaskMetrix extends Metrix{
	Date start = null; 
	Date down = null; 
	CopyOnWriteArrayList<ChildMetrix> childs = new CopyOnWriteArrayList<ChildMetrix>();
	CopyOnWriteArrayList<HostMetrix> hosts = new CopyOnWriteArrayList<HostMetrix>();
	public TaskMetrix(int taskid){
		this.id = taskid;
		state = MCODE.INIT;
	}
	public void checkTaskComplete(){
		int childnum = childs.size();
		for ( int i = 0;i < childnum;i++){
			if (childs.get(i).state != MCODE.FINISH){
				return;
			}
		}
		state = MCODE.FINISH;
	}
	public Map<Integer, String> taskchildinfo(){
		Map<Integer, String> map = new HashMap<Integer, String>();
		int childnum = childs.size();
		ChildMetrix tmp = null;
		for (int p = 0;p < childnum; p++){
			tmp = childs.get(p);
			map.put(tmp.id, tmp.works()+" "+tmp.percent());		
		}
		return map;
	}
	public Map<Integer, Integer> taskhostinfo(){
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		int childnum = hosts.size();
		HostMetrix tmp = null;		
		for (int p = 0;p < childnum; p++){
			tmp = hosts.get(p);
			map.put(tmp.id, tmp.childs.size());		
		}
		return map;		
	}
	public void pointTo(Metrix metrix){
		if ( metrix.getClass() == HostMetrix.class ){
			hosts.add((HostMetrix) metrix);
		}
		else if ( metrix.getClass() == ChildMetrix.class ){
			childs.add((ChildMetrix) metrix);
			if (state == MCODE.INIT){
				state = MCODE.RUNNING;
			}
		}
	}
	public void removePoint(Metrix metrix){

	}
}