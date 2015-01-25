package com.intel.fangpei.terminalmanager;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.intel.fangpei.BasicMessage.AppHandler;
import com.intel.fangpei.BasicMessage.BasicMessage;
import com.intel.fangpei.BasicMessage.HeartBeatMessage;
import com.intel.fangpei.BasicMessage.ServiceMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.NIOServerHandler;
import com.intel.fangpei.network.SelectionKeyManager;
import com.intel.fangpei.resource.metrix.ChildMetrix;
import com.intel.fangpei.resource.metrix.HostMetrix;
import com.intel.fangpei.resource.metrix.MCODE;
import com.intel.fangpei.resource.metrix.Metrix;
import com.intel.fangpei.resource.metrix.MetrixCircle;
import com.intel.fangpei.resource.metrix.TaskMetrix;
import com.intel.fangpei.util.Line;
import com.intel.fangpei.util.ServerUtil;
import com.intel.fangpei.util.TimeCounter;

/**
 * server use this class to communicate with Admin.
 * 
 * @author fangpei
 * 
 */
public class AdminManager extends SlaveManager {

	SelectionKey admin = null;
	MonitorLog ml = null;
	AppHandler handler = new AppHandler(1);

	public AdminManager(MonitorLog ml, SelectionKey admin,
			SelectionKeyManager keymanager, NIOServerHandler nioserverhandler) {
		super(keymanager, nioserverhandler);
		this.ml = ml;
		this.admin = admin;
	}

	public boolean Handle(packet p) {
		return Handle(admin, p);
	}

	public boolean Handle(SelectionKey admin, packet p) {
		buffer = p;
		unpacket();
		if (args != null)
			System.out.println("[AdminManager]"
					+ ((SocketChannel) admin.channel()).socket()
							.getInetAddress().getHostAddress() + ":"
					+ args + "[end]");
		if (command == BasicMessage.OP_QUIT) {
			ml.log("handle admin's quit request");
			packet one = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER,
					BasicMessage.OP_MESSAGE, "offline");
			nioserverhandler.pushWriteSegement(admin, one);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			nioserverhandler.removeWriteKey(admin);
			keymanager.setAdmin(null);
			keymanager.addCancelInterest(admin);
			return false;
		}
		if (command == BasicMessage.OP_SH) {
			ml.log("handle admin's sh request");
			HandOneNode();
			return true;
		}
		AllHandsHandler();
		return true;
	}

	private void HandOneNode() {
		String tmp = args.toStringUtf8();
		tmp = tmp.trim();
		String[] meta = tmp.split(" ");
		SelectionKey sk = keymanager.getOneNode(meta[0]);
		try {
			packet p = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER, command,
					tmp.substring(tmp.indexOf(" ")));
			nioserverhandler.pushWriteSegement(sk, p);
		} catch (StringIndexOutOfBoundsException e) {
			packet p = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER, command);
			nioserverhandler.pushWriteSegement(sk, p);
		}
		nioserverhandler.pushWriteSegement(admin, PacketProtocolImpl.CreatePacket(
				BasicMessage.SERVER, BasicMessage.OK));
	}

	public String AllHandsHandler() {
		packet p = null;
		switch (command) {
		case BasicMessage.OP_EXEC:
		case ServiceMessage.SERVICE:
		case ServiceMessage.THREAD:
			p = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER, command, args.toStringUtf8());
			ml.log("handle admin's exec|service|thread request");
			keymanager.handleAllNodes(nioserverhandler, p);
			break;
		case BasicMessage.OP_CLOSE:
		case BasicMessage.OP_MESSAGE:
		case BasicMessage.OP_SYSINFO:
			ml.log("handle admin's close|message|sysinfo request");
			p = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER, command, args.toStringUtf8());
			keymanager.handleAllNodes(nioserverhandler, p);
			packet reply = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER, BasicMessage.OK);
			nioserverhandler.pushWriteSegement(admin, reply);
			break;
		}
		return "";
	}

}
