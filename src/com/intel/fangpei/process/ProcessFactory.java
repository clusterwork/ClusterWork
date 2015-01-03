package com.intel.fangpei.process;
import java.util.Random;
import org.apache.commons.lang.ArrayUtils;
/**
 * build new proccess factory.
 * @author Administrator
 *
 */
public class ProcessFactory {
private static Proc oneproc = null;
private static Random rand = new Random();
private volatile static int processNum = 0;
private synchronized static void refreshID(){
	processNum = rand.nextInt(100000);
	System.out.println("the process id is:"+processNum);
}
public static int buildNewProcessWithProcessid(String... command){
	if(command == null){
		return -1;
	}
	refreshID();
	System.out.println("process id is:"+processNum);
	oneproc = new Proc((String[]) ArrayUtils.add(command, command.length, ""+processNum));
	ProcessManager.add(processNum, oneproc);
	return processNum;
	
}
public static int buildNewProcess(String... command){
	if(command == null){
		return -1;
	}
	refreshID();
	System.out.println("process id is:"+processNum);
	oneproc = new Proc(command);
	ProcessManager.add(processNum, oneproc);
	return processNum;
	
}
public static void setStartID(int processStartId){
	processNum = processStartId;
}
/**
 * added
 */
public static int getProcessNum(){
	return processNum;
}
}
