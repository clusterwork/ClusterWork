package com.intel.fangpei.task.handler;

import com.intel.fangpei.terminal.Node;

public abstract class Extender implements ExtendAttr {
	Node node = null;
	protected double percent = 0.0;
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

	public void commit() {
		commitSplit();
		percent = 1.0;
		//report this thread is complete.
	}

	@Override
	public abstract void commitSplit();
	
	public void setCompletePercent(double percent){
		this.percent = percent;
	}

}
