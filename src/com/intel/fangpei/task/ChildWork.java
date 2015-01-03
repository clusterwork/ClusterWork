package com.intel.fangpei.task;

import com.intel.fangpei.task.TaskRunner.SplitId;
import com.intel.fangpei.task.TaskRunner.SplitWork;
import com.intel.fangpei.task.TaskRunner.TaskEnv;
import com.intel.fangpei.util.Line;
import com.intel.fangpei.util.Line.segment;

public class ChildWork {
public int jvmId = 0;
TaskEnv env = null;
private boolean startNextThread = false;
Line<SplitId,SplitWork> works = new Line<SplitId,SplitWork>();
TaskRunner boss = null;
public ChildWork(int jvmId) {
	this.jvmId = jvmId;
}
public ChildWork(int jvmId,TaskEnv env) {
	this.jvmId = jvmId;
	this.env = env;
}
public void setEnv(TaskEnv env){
	this.env = env;
}
public void SetTaskRunner(TaskRunner tr){
	boss = tr;
}
/**
 * if use this function:
 * return true:
 * perform OK for start next work
 * return false:
 * already start a signal for start next work and the work has not been
 * performed OK!please wait...
 */
public synchronized boolean nextWork(){
	if(startNextThread){
		return false;
	}
	boss.report("[jvmtask]prepare to start next split task in the JVM:"+jvmId);
	startNextThread = true;
	return true;
}
public segment getSplit() {
	startNextThread = false;
	return works.popNode();
}
public int getId(){
	return jvmId;
}
public void assignNewSplit(SplitId id,SplitWork cr){
	cr.setEnv(env);
	works.addNode(id, cr);
}
public boolean noSplitAssign() {
	if(works!=null&&works.hasNext()){
		return false;
	}
	return true;
}

//public boolean canStartNextThread() {
//	if(startNextThread){
//		startNextThread = false;
//		return true;
//		}
//	return false;
//}
}
