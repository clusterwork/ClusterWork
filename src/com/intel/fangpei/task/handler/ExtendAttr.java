package com.intel.fangpei.task.handler;

public interface ExtendAttr {
	public void commitSplit();
	public double getCompletePercent();
	public abstract String reportStatus();
}
