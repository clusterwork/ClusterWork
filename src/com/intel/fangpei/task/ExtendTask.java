package com.intel.fangpei.task;

import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.task.handler.Extender;
import com.intel.fangpei.util.ReflectFactory;

public class ExtendTask implements Runnable{
int jvmId = 0;
RpcClient rpcClient = null;
MonitorLog ml = null;
ClassLoader ccl =Thread.currentThread().getContextClassLoader();
String classname = null;
String args[] = null;
Extender eh = null;
double completePercent = 0.0;
public ExtendTask(int jvmId,RpcClient rpcClient,MonitorLog ml,String classname){
	this.jvmId = jvmId;
	this.rpcClient =rpcClient;
	this.ml = ml;
	this.classname = classname;
}
public ExtendTask(int jvmId,RpcClient rpcClient,MonitorLog ml,String classname,String[] args){
	this.jvmId = jvmId;
	this.rpcClient =rpcClient;
	this.ml = ml;
	this.classname = classname;
	this.args = args;
}
public String taskname(){
	return classname;
}
@Override
public void run() {
	ReflectFactory rf = ReflectFactory.getInstance();
	try{
		if(args == null){
		eh = (Extender) rf.getMyInstance(classname);
		}else{
		eh = (Extender) rf.getMyInstance(classname, args);
		}
	}catch(Exception e){
		//ml.log("Exception: fail to load class "+e.getMessage()+" please " +
		//		"check the Path ");
		try {
			eh = (Extender) rf.getMyInstance("com.intel.developer.extend.Command",classname.replace("com.intel.developer.extend.", ""));
		} catch (Exception e1) {
			ml.error("no command named:"+classname.replace("com.intel.developer.extend.", ""));
		}
	}
	eh.commit();	
	
}
public boolean setCompletePercent(){
	if(eh == null){
		return false;
	}
	double percent = eh.getCompletePercent();
	if(completePercent == percent){
		ml.log("same percent,so not report!");
		return false;
	}
	Object[] params = new Object[] { jvmId,classname,percent };
	rpcClient.execute("ChildHandler.splitPercent", params);
	
	if(percent > 0.99999){
		params = new Object[] { jvmId };
		rpcClient.execute("ChildHandler.completesplit", params);
	}
	completePercent = percent;
	return percent > 0.99999;
}

}
