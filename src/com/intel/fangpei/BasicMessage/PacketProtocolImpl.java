package com.intel.fangpei.BasicMessage;
import java.nio.ByteBuffer;

import com.clusterwork.protocol.PacketProtos.packet;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
public class PacketProtocolImpl {
	public static packet CreatePacket(int clientType,int command,String args){
		ByteString tmp = ByteString.copyFromUtf8(args);
		int len = tmp.size();
		packet.Builder builder = packet.newBuilder();
		builder.setVersion(BasicMessage.VERSION);
		builder.setArgsize(len);
		builder.setClientType(clientType);
		builder.setCommand(command);
		builder.setArgs(tmp);
		return builder.build();	
	}
	public static packet CreatePacket(ByteBuffer buffer){
		buffer.flip();
		byte[] dst = new byte[buffer.limit()]; 
		buffer.get(dst, 0, buffer.limit());
		packet p = null;
		try {
			p = packet.parseFrom(dst);
			return p;
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	public static packet CreatePacket(int clientType,int command){
		packet.Builder builder = packet.newBuilder();
		builder.setVersion(BasicMessage.VERSION);
		builder.setArgsize(0);
		builder.setClientType(clientType);
		builder.setCommand(command);
		return builder.build();	
	}
	
}
