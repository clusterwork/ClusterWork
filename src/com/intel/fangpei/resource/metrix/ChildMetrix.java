package com.intel.fangpei.resource.metrix;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.intel.fangpei.resource.metrix.HostMetrix;
import com.intel.fangpei.resource.metrix.Metrix;
import com.intel.fangpei.resource.metrix.TaskMetrix;
import com.intel.fangpei.util.Line;

public class ChildMetrix extends Metrix{
	//all loads
	int  loads = 0;
	//now,working load?
	String working = "";
	//already complete loads;
	Line<String,String[]> splits = new Line<String,String[]>();
	Map<String,Double> splitProcess = new HashMap<String,Double>();
	int completeworks = 0;
	long start = 0; 
	long down = 0; 
	HostMetrix host = null;
	TaskMetrix task = null;
	public ChildMetrix(int childid){
		id = childid;
		state = MCODE.INIT;
	}
	public void addLoad(String classname,String[] args){
		splits.addNode(classname, args);
		loads++;
	}
	public void pointTo(Metrix metrix){
		if ( metrix.getClass() == HostMetrix.class ){
			host = (HostMetrix) metrix;
		}
		else if ( metrix.getClass() == TaskMetrix.class ){
			task = (TaskMetrix) metrix;
			state = MCODE.RUNNING;
		}
	}
	public void startone(){
		if(start == 0){
			start = System.currentTimeMillis();
		}
		working+="+"; 
	}
	public void setProcess(String classname,Double percent){
		splitProcess.put(classname, percent);
	}
	public void completeone(){
		completeworks ++;
		working = working.substring(1);
		if (completeworks == loads){
			state = MCODE.FINISH;
			down = System.currentTimeMillis();
			//Hook for task to check complete.
			task.checkTaskComplete();
		}
	}
	public double percent(){
		if (loads > 0.0){
			return completeworks/(loads+0.0);
		}
		return 1.0;
	}
	public String works(){
		String works = "";
		for(int i =0;i<loads;i++){
			works+=splits.get(i).k+" "+ splitProcess.get(splits.get(i).k) + "\n";
		}
		return works;
	}
	public String toString(){
		return "State:" + MCODE.CODESTR.getName(state) +
	            "\nWorking:"+working+
				"\nSplit Names:\n"+ works() +
				"CompleteNum:"+ completeworks+ 
				"\nLoadNum:"+loads+
				"\nStartTime:"+start+
				"\nDownTime:"+down+
				"\nSplit Compelete Percent:"+percent();
	}
}