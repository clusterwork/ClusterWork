package com.intel.fangpei.network;


import com.intel.fangpei.BasicMessage.BasicMessage;
import com.intel.fangpei.BasicMessage.HeartBeatMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.util.ConfManager;

public class HeartBeatThread implements Runnable{
	
	public  static int HEART_BEAT_INTERVAL = 5;
	public static int HEART_BEAT_OUT_TIME = 10;
	private NIOHandler connect;
	private String hostname;
	public HeartBeatThread(NIOHandler connect,String hostname){
		ConfManager.addResource(null);
		HEART_BEAT_INTERVAL= ConfManager.getInt("heartbeat.interval", HEART_BEAT_INTERVAL);
		HEART_BEAT_OUT_TIME= ConfManager.getInt("heartbeat.outtime", HEART_BEAT_OUT_TIME);
		this.connect=connect;
		this.hostname=hostname;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			while(true){
			packet heart_beat = PacketProtocolImpl.CreatePacket(BasicMessage.NODE, HeartBeatMessage.HEART_BEAT,hostname);
			connect.addSendPacket(heart_beat);
			Thread.sleep(HEART_BEAT_INTERVAL*1000);
			//packet heart_beat_check = new packet(BasicMessage.ADMIN, HeartBeatMessage.HEART_BEAT_CHECK);
			//connect.addSendPacket(heart_beat_check);
			//System.out.println("one heart beat done!");
		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
