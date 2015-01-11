package com.intel.fangpei.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

import com.intel.fangpei.BasicMessage.BasicMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.terminal.Admin;
import com.intel.fangpei.util.TimeCounter;

public class NIOHandler implements IConnection, INIOHandler, Runnable {
	protected MonitorLog ml = null;
	protected String serverip = null;
	protected int port = 0;
	protected SocketAddress address = null;
	protected SocketChannel channel = null;
	protected ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 4);
	private TimeCounter tc = null;
	private LinkedList<packet> sendqueue = new LinkedList<packet>();
	private LinkedList<packet> receivequeue = new LinkedList<packet>();
	private int version = 0, argsize = 0;
	byte[] args = null;
	private int clientType = 0, command = 0;
	public NIOHandler(String ip, int port) {
		this.serverip = ip;
		this.port = port;
		try {
			ml = new MonitorLog();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tc = new TimeCounter(5000);
		buffer.clear();
		buffer.flip();
	}

	@Override
	public void processConnect() throws IOException {
		address = new InetSocketAddress(serverip, port);
		channel = SocketChannel.open(address);
		channel.configureBlocking(false);
	}
	/***
	 * the function will sync block until 
	 * there have any data to be received
	 * @return true if you can read packet now.
	 */
	public boolean waitReadNext(){
		synchronized(receivequeue){
		if(isEmpty()){
			try {
				receivequeue.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
		}
	}
	@Override
	public synchronized void processRead() throws IOException {
		packet p = receive();
		if(p == null){
			return;
		}
		if(Admin.debug)
		System.out.println("[NIOHandler]read a packet:"+p);
		 synchronized(receivequeue){
			receivequeue.addLast(p);
			receivequeue.notifyAll();
		 }
	}

	@Override
	public synchronized void processWrite() throws IOException {
		if (sendqueue.isEmpty())
			return;
		packet p = sendqueue.pop();
		byte[] tmpArray = p.toByteArray();
		int msgLen = tmpArray.length;
		buffer.clear();
		buffer.limit(Integer.SIZE+msgLen);
		System.out.println("buffer is:"+buffer+",limit:"+(Integer.SIZE+msgLen));
		buffer.putInt(msgLen);
		System.out.println("buffer is:"+buffer);
		buffer.put(tmpArray,0,msgLen);
		System.out.println("buffer is:"+buffer);
		buffer.flip();
		System.out.println("buffer is:"+buffer);
		send(buffer);
		//System.out.println("[NIOHandler]processwrite:");
	}

	@Override
	public void processError(Exception e) {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized void addSendPacket(packet out) {
		sendqueue.add(out);
	}

	public synchronized packet getReceivePacket() {
		if (receivequeue.isEmpty())
			return null;
		return receivequeue.pop();
	}

	@Override
	public void clearSendQueue() {
		sendqueue.clear();

	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return (InetSocketAddress) address;
	}

	@Override
	public SelectableChannel channel() {
		// TODO Auto-generated method stub
		return channel;
	}

	@Override
	public INIOHandler getNIOHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return receivequeue.isEmpty();
	}

	@Override
	public packet receive() throws IOException {
		buffer.clear();
		buffer.limit(Integer.SIZE);
		channel.read(buffer);
		if(buffer.position() == 0){
			System.out.println("no data received");
			return null;
		}
		buffer.flip();
		int msgLen = buffer.getInt();
		System.out.println("[NIOHandler]packet len:"+msgLen);
		buffer.clear();
		buffer.limit(msgLen);
		while(buffer.hasRemaining()){
			channel.read(buffer);
		}
		System.out.println("[NIOHandler][receive]get buffer:"+buffer);
		packet p = PacketProtocolImpl.CreatePacket(buffer);
		return p;
	}

	@Override
	public void send(ByteBuffer buffer) {
		while (buffer.hasRemaining()) {
			try {
				int len = channel.write(buffer);
				if(Admin.debug)
				ml.log("[NIOHandler]send a packet:"+buffer+",len:"+len);
			} catch (IOException e) {
				ml.error("send data to server failed:"+e.getMessage());
				break;
			}
		}
	}

	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}

	@Override
	public void run() {

	}

}
