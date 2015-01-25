package com.intel.fangpei.network.rpc;

import com.intel.fangpei.resource.metrix.HostMetrix;

public class HostHandler {
	private ServerRunningResource stm;
	public HostHandler(){
		stm  = ServerRunningResource.getServerTaskMonitorInstance();
	}
	public boolean newHost(int hostid,String ip){
		stm.newHost(hostid,ip);
		return true;
	}
	public String getHost(int hostid){
		return stm.getHost(hostid);
	}
	public Object[] getHosts(){
		return stm.getHosts();
	}
	public String getHostSummary(){
		return stm.getHostSummary();
	}
	
}
