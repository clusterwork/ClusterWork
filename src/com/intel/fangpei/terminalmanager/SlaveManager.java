package com.intel.fangpei.terminalmanager;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.google.protobuf.ByteString;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.NIOServerHandler;
import com.intel.fangpei.network.SelectionKeyManager;

public abstract class SlaveManager {
	MonitorLog ml = null;
	SelectionKey key = null;
	SelectionKeyManager keymanager = null;
	packet buffer = null;
	int version = 0;
	int argsize = 0;
	int clientType = 0;
	int command = 0;
	ByteString args = null;
	NIOServerHandler nioserverhandler = null;
	public SlaveManager(SelectionKeyManager keymanager,NIOServerHandler nioserverhandler) {
		try {
			ml = new MonitorLog();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.keymanager = keymanager;
		this.nioserverhandler = nioserverhandler;
	}

	public abstract boolean Handle(SelectionKey key,packet p);

	protected void unpacket() {
		version = buffer.getVersion();
		argsize = buffer.getArgsize();
		clientType = buffer.getClientType();
		command = buffer.getCommand();
		args = buffer.getArgs();
	}

}
