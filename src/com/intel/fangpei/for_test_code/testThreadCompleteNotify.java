package com.intel.fangpei.for_test_code;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.intel.fangpei.util.TimeCounter;

public class testThreadCompleteNotify {
public class Metrix {
	int state = 0;
	public void pointTo(Metrix metrix){

	}
	public void removePoint(Metrix metrix){

	}
}
public class ChildMetrix extends Metrix{
	int childid = 0;
	//all loads
	int  loads = 0;
	//now,working load?
	String working = "";
	//already complete loads;
	int completeworks = 0;
	Date start = null; 
	Date down = null; 
	HostMetrix host = null;
	TaskMetrix task = null;
	public ChildMetrix(int loads){
		this.loads = loads;
		 
	}
	public void pointTo(Metrix metrix){
		if ( metrix.getClass() == HostMetrix.class ){
			host = (HostMetrix) metrix;
		}
		else if ( metrix.getClass() == TaskMetrix.class ){
			task = (TaskMetrix) metrix;
		}
	}
	public void startone(){
		if(start == null){
			//start = 
		}
		working+="+"; 
	}
	public void completeone(){
		completeworks ++;
		working = working.substring(1);
		if (completeworks == loads){
			state = 2;
			//down = 
		}
	}
}
public class TaskMetrix extends Metrix{
	int taskid = 0;
	Date start = null; 
	Date down = null; 
	CopyOnWriteArrayList<ChildMetrix> childs = new CopyOnWriteArrayList<ChildMetrix>();
	CopyOnWriteArrayList<HostMetrix> hosts = new CopyOnWriteArrayList<HostMetrix>();
	public void pointTo(Metrix metrix){
		if ( metrix.getClass() == HostMetrix.class ){
			hosts.add((HostMetrix) metrix);
		}
		else if ( metrix.getClass() == ChildMetrix.class ){
			childs.add((ChildMetrix) metrix);
		}
	}
	public void removePoint(Metrix metrix){

	}
}
public class HostMetrix extends Metrix{
	int hostid = 0;
	Date start = null; 
	Date down = null; 
	CopyOnWriteArrayList<ChildMetrix> childs = new CopyOnWriteArrayList<ChildMetrix>();
	CopyOnWriteArrayList<TaskMetrix> tasks = new CopyOnWriteArrayList<TaskMetrix>();
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
//one metric description
public class MetrixCylinder {
	int splitNum = 0 ;
	Class type = null;
	CopyOnWriteArrayList<Metrix>[] splits = null;
	public MetrixCylinder(Class type,String[] names){
		splitNum = names.length;
		setType(type);
		splits = new CopyOnWriteArrayList[splitNum];
		for ( int i =0;i<splitNum;i++){
			splits[i] = new CopyOnWriteArrayList();
		}
		Thread t = new Thread(){
			public void run(){
				TimeCounter tc = new TimeCounter();
				while(tc.stay(5000)){
					freshSplit();
				}
			}
		};
		t.setDaemon(true);
		t.start();
}
	protected void freshSplit() {
		for (int stateid = 0;stateid < splitNum;stateid++){
			CopyOnWriteArrayList<Metrix> list = splits[stateid];
			int num = list.size();
			
			for (int j = 0;j < num;j++){
				if(list.get(j).state== stateid){
					continue;
				}
				else{
					int newstate = list.get(j).state;
					if(newstate < splitNum){
						splits[newstate].add(list.get(j));
						//delete from old split
						list.remove(j);
					}
					else{
						list.remove(j);
						System.out.println("[freshsplit] split state out of range,remove!");
					}
				}
			}
		}
		
	}
	public void setType(Class type){
		this.type = type;
		//checkAllTypeMatch();
	}
	public Metrix add(Metrix metrix,int state){
		if ( state >= splitNum || metrix == null){
			//out of control state
			System.out.println("state is null or not support state!");
			return null;
		}
		//check type?
		if ( type != null ){
			if ( metrix.getClass() != type ){
				System.out.println("type not matched!");
				return null;
			}
		}
		if (splits == null){
			System.out.print("split is null");
		}
		splits[state].add(metrix);
		return metrix;
	}
// should support remove?
//	public void remove(Metrix metrix){
//		if (metrix == null){
//			System.out.println("Metrix is null!");
//			return;
//		}
//	}
	
}
public class MetrixCircle extends Thread{
	public MetrixCylinder HOST = new MetrixCylinder(HostMetrix.class,new String[]{"down","running","other"});
	public MetrixCylinder TASK = new MetrixCylinder(TaskMetrix.class,new String[]{"finish","interrupt","waiting","running","other"});
	public MetrixCylinder CHILD = new MetrixCylinder(ChildMetrix.class,new String[]{"running","interrupt","finish","other"});
	public void newChild(ChildMetrix metrix,int state){
		CHILD.add(metrix, state);
	}
	public void newTask(TaskMetrix metrix,int state){
		TASK.add(metrix, state);		
	}
	public void newHost(HostMetrix metrix,int state){
		HOST.add(metrix, state);		
	}
	public void connect(Metrix A,Metrix B){
		A.pointTo(B);
		B.pointTo(A);
	}
	public ChildMetrix findChild(int childid){
		ChildMetrix result = null;
		for ( int i = 0 ; i < CHILD.splitNum; i++){
			if ((result=findChild(childid,i)) != null){
				return result;
			}
		}
		return result;
	}
	public ChildMetrix findChild(int childid,int state){
		if ( state >= CHILD.splitNum){
			return null;
		}
		CopyOnWriteArrayList<Metrix> split = CHILD.splits[state];
		int len = split.size();
		for ( int i = 0 ; i < len; i++){
			if ( ((ChildMetrix)split.get(i)).childid == childid ) {
				return (ChildMetrix) split.get(i);
			}
		}
		return null;
	}
	public void run(){
		//do check state change
	}
}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testThreadCompleteNotify notify = new testThreadCompleteNotify();
		MetrixCircle cirle= notify.new MetrixCircle();
		ChildMetrix child = notify.new ChildMetrix(5);
		child.childid=9;
		HostMetrix host = notify.new HostMetrix();
		host.hostid = 8;
		TaskMetrix task = notify.new TaskMetrix();
		task.taskid = 7;
		cirle.newChild(child, 0);
		cirle.newHost(host, 0);
		cirle.newTask(task, 0);
		cirle.connect(task, host);
		cirle.connect(task, child);
		cirle.connect(host, child);
		child.startone();
		child.startone();
		child.startone();
		child.startone();
		child.startone();
		child.completeone();
		child.completeone();
		child.completeone();
		child.completeone();
		child.completeone();
		int a = 0;
		a++;
	}
}

