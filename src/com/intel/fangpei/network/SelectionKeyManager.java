package com.intel.fangpei.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.util.ConfManager;
import com.intel.fangpei.util.ServerUtil;
/**
 * manager all cluster connection keys on this server.
 * @author Administrator
 *
 */
public class SelectionKeyManager {
	private MonitorLog ml = null;
	
	
	// the cluster's nodes list
	protected HashMap<SelectionKey,String> nodes = new HashMap<SelectionKey,String>();
	// the keys that is needed to process
	private LinkedList<SelectionKey> keys = new LinkedList<SelectionKey>();
	private SelectionKey lastone = null;
	protected LinkedList<SelectionKey> keys_read = new LinkedList<SelectionKey>();
	protected LinkedList<SelectionKey> keys_cancel = new LinkedList<SelectionKey>();
	private LinkedList<SelectionKey> receivequeue = new LinkedList<SelectionKey>();
	private SelectionKey Admin = null;
	
	//the heart beat list
	private HashMap<SelectionKey,Long> heart_list = new HashMap<SelectionKey,Long>();
	private HashMap<SelectionKey, String> host_list = new HashMap<SelectionKey, String>();

	public SelectionKey getAdmin() {
		return Admin;
	}

	public void setAdmin(SelectionKey admin) {
		/*
		 * this function will cancel the orignal Admin key if exist ,and replace
		 * it with admin no matter whether admin is null
		 */
		synchronized (keys) {
			if (Admin == null) {
				Admin = admin;
				keys.notify();
				return;
			}
			if (Admin.equals(admin)) {
				keys.notify();
				return;
			}
			Admin.cancel();
			Admin = admin;
			keys.notify();
		}
	}

	public SelectionKeyManager() {
		ConfManager.addResource(null);
		try {
			ml = new MonitorLog();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void pushKey(SelectionKey sc) {
		synchronized (keys) {
			if ((lastone != null) && sc.equals(lastone)) {
				keys.notify();
				return;
			}
			keys.add(sc);
			lastone = sc;
			keys.notify();
			return;
		}
	}

	public void pushKeys(Collection<SelectionKey> nodes) {
		Iterator<SelectionKey> i = nodes.iterator();
		SelectionKey key = null;
		synchronized (keys) {
			while (i.hasNext()) {
				key = i.next();
				keys.add(key);
			}
			lastone = key;
			keys.notify();
		}
	}

	public SelectionKey popKey() {
		SelectionKey key = null;
		synchronized (keys) {
			if (keys.isEmpty()) {
				return null;
			}
			key = keys.remove();
			if (keys.isEmpty()) {
				lastone = null;
			}
			keys.notify();
		}
		return key;

	}

	public boolean handleAllNodes(NIOServerHandler nioserverhandler,packet p) {
		Iterator<SelectionKey> i = nodes.keySet().iterator();
		SelectionKey key = null;
		while (i.hasNext()) {
			key = i.next();
			nioserverhandler.pushWriteSegement(key, p);
		}
		ml.log("handle all nodes with the command " + p.getCommand());
		return true;
	}

	public void addnode(SelectionKey key) {
		synchronized (nodes) {
			String hostname = ((SocketChannel)key.channel()).socket().getInetAddress().getHostName();
			if(hostname ==null)hostname = "unknown";
			nodes.put(key,hostname);
			ml.log("add one node form "+hostname+":"
					+ ((SocketChannel) key.channel()).socket().getInetAddress()
							.getHostAddress());
			nodes.notify();
		}

	}

	public void deletenode(SelectionKey key) {
		synchronized (nodes) {
			if (keys_read.contains(key)) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			keys_read.remove(key);
			//keys_write.remove(key);
			keys_cancel.remove(key);
			nodes.remove(key);
			try {
				key.channel().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			key.cancel();
			ml.log("delete one node form "
					+ ((SocketChannel) key.channel()).socket().getInetAddress()
							.getHostAddress());
			nodes.notify();
		}
	}

	public boolean findnode(SelectionKey key) {
		return nodes.keySet().contains(key);
	}

	public boolean isNoNodes() {
		return nodes.isEmpty();
	}

	public SelectionKey getOneNode() {

		return nodes.keySet().iterator().next();

	}
	public SelectionKey getOneNode(String hostname){
		
		if(!nodes.isEmpty()&&nodes.containsValue(hostname)){
			Iterator<SelectionKey> i = nodes.keySet().iterator();
			SelectionKey used = null;
			while(i.hasNext()){
				used = i.next();
				System.out.println(nodes.get(used));
				if(nodes.get(used).equals(hostname)){
					return used;
				}
			}
			ml.warn("cann't find your request node:"+hostname);
			return null;
		}
		ml.warn("you try to find a node named :"+hostname+",but nodes is empty or we don't have this node");
		return null;
	}
	public synchronized boolean addReadInterest(SelectionKey key) {
		synchronized (keys_read) {
			keys_read.add(key);
			keys_read.notify();
			return true;
		}
	}

	public SelectionKey popNeedReadKey() {
		synchronized (keys_read) {
		if (keys_read.isEmpty()) {
			return null;
		}
		SelectionKey key = keys_read.remove();
		keys_read.notify();
		return key;
		}
	}

	public synchronized boolean addCancelInterest(SelectionKey key) {
		synchronized (keys_cancel) {
			keys_cancel.add(key);
			keys_cancel.notify();
			return true;
		}
	}

	public SelectionKey popNeedCancelKey() {
		synchronized (keys_cancel) {
		if (keys_cancel.isEmpty()) {
			return null;
		}
			SelectionKey key = keys_cancel.remove();
			keys_cancel.notify();
			return key;
		}
	}
	public synchronized boolean addNeedProcessKey(SelectionKey key){
			receivequeue.add(key);
			return true;
		}
	public synchronized SelectionKey popNeedProcessKey(){
			if(receivequeue.isEmpty()){
				return null;
			}
			SelectionKey key = receivequeue.remove();
			return key;
	}
	public void registeHeartBeat(SelectionKey key,String hostname){
		synchronized (heart_list) {	
				long now_time = System.currentTimeMillis();
				heart_list.put(key, now_time);
		}
	
	}
	public void checkHeartBeat(){
		int timeout = HeartBeatThread.HEART_BEAT_OUT_TIME;
		synchronized (heart_list) {
			Iterator<Entry<SelectionKey, Long>> itr = heart_list.entrySet().iterator();
			while(itr.hasNext()){
				Entry<SelectionKey, Long> node_record = itr.next();
				//System.out.println("Node have "+node_record.getKey().toString()+"--"+nodes.get(node_record.getKey()));
				long now_time = System.currentTimeMillis();
				if(now_time -node_record.getValue()>timeout*1000){
					ml.log("Node "+nodes.get(node_record.getKey())+" is offline!");
					heart_list.remove(node_record.getKey());
					RpcClient rpcClient = RpcClient.getInstance();
					if( ((SocketChannel)node_record.getKey().channel()).socket().getInetAddress()==null){
						System.out.println("rpc client is null!");
					}
					rpcClient.execute("TaskHandler.NotifyNodeTimeout", ((SocketChannel)node_record.getKey().channel()).socket().getInetAddress().getHostAddress());
					this.addCancelInterest(node_record.getKey());
				}
			}
			
		}
	}
	
}
