package com.intel.fangpei.resource.metrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.resource.metrix.HostMetrix;
import com.intel.fangpei.resource.metrix.Metrix;
import com.intel.fangpei.resource.metrix.MetrixCylinder;
import com.intel.fangpei.resource.metrix.TaskMetrix;
import com.intel.fangpei.util.TimeCounter;

public class MetrixCircle extends Thread{
	public MetrixCircle(){
		Thread t = new Thread(){
			public void run(){
				TimeCounter tc = new TimeCounter();
				while(tc.stay(5000)){
					HOST.freshSplit();
					TASK.freshSplit();
					CHILD.freshSplit();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	public MetrixCylinder HOST = new MetrixCylinder(HostMetrix.class);
	public MetrixCylinder TASK = new MetrixCylinder(TaskMetrix.class);
	public MetrixCylinder CHILD = new MetrixCylinder(ChildMetrix.class);
	public void newChild(ChildMetrix metrix,int state){
		CHILD.add(metrix, state);
	}
	public void newTask(TaskMetrix metrix,int state){
		synchronized(TASK){
		TASK.add(metrix, state);
		TASK.notify();
		}
	}
	public void newHost(HostMetrix metrix,int state){
		HOST.add(metrix, state);		
	}
	public void connect(Metrix A,Metrix B){
		if (A == null || B == null){
			System.out.println("[MetrixCircle] connect with null.");
			if(B == null&& A == null){
				System.out.println("*******************************ALL NONE");
			}
			return;
		}
		A.pointTo(B);
		B.pointTo(A);
	}
	public HostMetrix findHost(int id){
		return (HostMetrix)HOST.find(id);
	}
	public ChildMetrix findChild(int id){
		return (ChildMetrix)CHILD.find(id);
	}
	public TaskMetrix findTask(int id){
		return (TaskMetrix)TASK.find(id);
	}
	public Object[] hosts(){
		return HOST.IDs();
	}
	public Object[] childs(){
		return CHILD.IDs();
	}
	public String hostSummary(){
		return HOST.toString();
	}
	public String childSummary(){
		return CHILD.toString();
	}
	public String taskSummary(){
		return TASK.toString();
	}
	public Object[] retrivetasks(){
		return TASK.IDs();
	}
}
