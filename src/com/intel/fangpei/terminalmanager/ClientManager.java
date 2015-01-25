package com.intel.fangpei.terminalmanager;

import java.nio.channels.SelectionKey;

import com.intel.fangpei.BasicMessage.BasicMessage;
import com.intel.fangpei.BasicMessage.HeartBeatMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.network.NIOServerHandler;
import com.intel.fangpei.network.SelectionKeyManager;
import com.intel.fangpei.network.rpc.RpcClient;
import com.intel.fangpei.util.SystemUtil;
/**
 * server use this class to communicate with Node.
 * @author fangpei
 *
 */
public class ClientManager extends SlaveManager{
	volatile static int  nodeid = 1000;
	public ClientManager(SelectionKeyManager keymanager,NIOServerHandler nioserverhandler) {
		super(keymanager,nioserverhandler);
	}

	public boolean Handle(SelectionKey key,packet p){
		//System.out.println("[ClientManager]handle this request");
		this.key = key;
		buffer = p;
		//System.out.println("[ClientManager]this client packet is:"+SystemUtil.byteToString(p.getArgs()));
			unpacket();
			if (clientType == BasicMessage.ADMIN) {
				if (command == BasicMessage.OP_QUIT) {
					keymanager.setAdmin(null);
					key.cancel();
					return false;
				}
				if (command == BasicMessage.OP_LOGIN) {
					packet p2 = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER,BasicMessage.OP_MESSAGE,"[message]admin");
					nioserverhandler.pushWriteSegement(key, p2);
					keymanager.setAdmin(key);
					ml.log("New admin login!");
				}
			} else if (clientType == BasicMessage.NODE) {
				SelectionKey admin = keymanager.getAdmin();
				if (command == BasicMessage.OP_QUIT) {
					keymanager.deletenode(key);
					return false;
				}
				if (command == BasicMessage.OP_LOGIN) {
					nodeid++;
					packet p2 = PacketProtocolImpl.CreatePacket(BasicMessage.SERVER,BasicMessage.OP_LOGIN,""+nodeid);
					nioserverhandler.pushWriteSegement(key, p2);
					keymanager.addnode(key);
				}
				if(command ==BasicMessage.OP_SYSINFO){
					nioserverhandler.pushWriteSegement(admin, buffer);
					//HashMap<String,String> hm = SysInfo.deserialize(args);
					//System.out.println(hm.get("CPU_Vendor"));
					//System.out.println(hm.get("Disk_info"));
					//System.out.print(hm.get("RegUser"));
					return true;
				}
				if(command ==BasicMessage.OP_MESSAGE){
					System.out.println("[ClientManager]add NIO write interest for Admin");
					nioserverhandler.pushWriteSegement(admin,  buffer);
					return true;
				}
				//add what to do when get node heart beat call back segment
				if(command == HeartBeatMessage.HEART_BEAT){
				//	System.out.println("registe node key");
				//	System.out.println(new String(args));
					keymanager.registeHeartBeat(key,args.toStringUtf8());
				//	System.out.println(key.toString()+"------"+keymanager.getAdmin().toString());
					return true;
				}
			}
			if (!operate()) {
				return false;
			}
		return true;
	}

	private boolean operate() {
		System.out.println("normal process");
		return true;
	}

}
