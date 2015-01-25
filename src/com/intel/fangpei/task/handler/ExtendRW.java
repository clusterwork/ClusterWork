package com.intel.fangpei.task.handler;

import com.intel.fangpei.task.DataSourcePool;
import com.intel.fangpei.terminal.Node;

public abstract class ExtendRW<type extends Object> implements ExtendAttr {
	Node node = null;
	DataSourcePool<type> buffer = null;
	DataSourcePool<type> dataPool = null;
	protected double percent = 0.0;
	private String sourceSpaceID = null;
	private String writeDir = null;
	public ExtendRW(String sourceSpaceID){
		this.sourceSpaceID  = sourceSpaceID;
	}
	public ExtendRW(String sourceSpaceID,String writeDir){
		this.sourceSpaceID  = sourceSpaceID;
		this.writeDir =writeDir;
	}
	@Override
	public double getCompletePercent() {
		return percent;
	}
	public void setFather(Node node){
		this.node = node;
	}
	@Override
	public String reportStatus() {
		// TODO Auto-generated method stub
		return "[extend ExtendHandler] auto";
	}

	public void run() {
		commitSplit();
		percent = 1.0;
	}

	@Override
	public abstract void commitSplit();

}
