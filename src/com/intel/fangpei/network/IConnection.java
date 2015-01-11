package com.intel.fangpei.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;

public interface IConnection {
	public void addSendPacket(packet out);

	public void clearSendQueue();

	public String getId();

	public void dispose();

	public InetSocketAddress getRemoteAddress();

	public SelectableChannel channel();

	public INIOHandler getNIOHandler();

	public boolean isEmpty();

	public packet receive() throws IOException;

	public void send(ByteBuffer buffer);

	public boolean isConnected();
}
