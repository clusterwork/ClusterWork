package com.intel.fangpei.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import com.intel.fangpei.BasicMessage.BasicMessage;
//import com.intel.fangpei.BasicMessage.packet;
import com.clusterwork.protocol.PacketProtos.packet;
import com.intel.fangpei.BasicMessage.PacketProtocolImpl;
import com.intel.fangpei.logfactory.MonitorLog;
import com.intel.fangpei.network.PacketLine.segment;
import com.intel.fangpei.terminal.Admin;
import com.intel.fangpei.util.ConfManager;
import com.intel.fangpei.util.Line;
import com.intel.fangpei.util.ServerUtil;
import com.intel.fangpei.util.SystemUtil;
import com.intel.fangpei.util.TimeCounter;
/**
 * Start as common server ,the args contains port.
 * <p>this class also proccess server connection.It uses SelectionKeyManager to manager it's keys.</p>
 */
public class NIOServerHandler implements INIOHandler,Runnable{
	PacketLine pipeline = null;
	PacketLine waitWritePipeLine = null;
	Selector selector = null;
	private SelectionKeyManager manager = null;
	private SelectionKey inprocesskey = null;
	private ByteBuffer buffer = null;
	private int version = 0;
	private int clientType = 0;
	private int command = 0;
	private int argsize = 0;
	private byte[] args = null;
	private packet p = null;
	private TimeCounter tc = null;
	private MonitorLog ml = null;
	private int port = 0;
	ServerSocketChannel serverchannel = null;
	public NIOServerHandler(int port,MonitorLog ml,SelectionKeyManager manager){
		this.port = port;
		ConfManager.addResource(null);
		this.manager  = manager;
		if(ml!=null){
		this.ml = ml;
		}else{
			try {
				ml = new MonitorLog();
				System.out.println("init ml:"+ml);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		buffer = ByteBuffer.allocate(1024*1024*4);
		buffer.clear();
		tc = new TimeCounter(10000);
		pipeline = new PacketLine();
		waitWritePipeLine = new PacketLine();
	}
	public NIOServerHandler(int port,MonitorLog ml){
		this(port,ml,new SelectionKeyManager());
	}
	@Override
	public void processConnect() throws IOException {
		// it have been processed by selectsocket ;

	}

	@Override
	public void processRead() throws IOException {
			//System.out.println("[NIOServerHandler]read a packet");
			SocketChannel channel = (SocketChannel) inprocesskey.channel();
			packet p = null;
			try{
			if((p = receive(channel)) == null){
				System.out.println("read node from "
						+ channel.socket().getInetAddress().getHostAddress()
						+ " fail,cancel this key!");
				manager.addCancelInterest(inprocesskey);
				return;
			}
			}catch(IOException e){
				System.out.println("IO Exception:read node from "
						+ channel.socket().getInetAddress().getHostAddress()
						+ " fail,exclude the node!");
				manager.addCancelInterest(inprocesskey);
				return;
			}
				//add until true
				while(!pipeline.addNode(inprocesskey, p)){
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				argsize = 0; 
		}

	//}

	@Override
	public void processWrite() throws IOException {
		while (waitWritePipeLine.hasNext()) {
			segment se = waitWritePipeLine.popNode();
			if (se != null) {
				SelectionKey sk = se.key;
				packet p = se.p;
				SocketChannel channel = (SocketChannel) sk.channel();
				int msgLen = p.getSerializedSize();
				ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE+msgLen);
				buffer.putInt(msgLen);
				buffer.put(p.toByteArray());
				buffer.flip();
				send(channel,buffer);
//				if (buffer != null) {
//					int len = channel.write((ByteBuffer) buffer);
//					System.out
//							.println("[NIOServerHandler]write out packet:"
//									+ p+",bytes:"+len);
//				}
			}

		}
	}

	public void send(SocketChannel channel,ByteBuffer buffer) {
		while (buffer.hasRemaining()) {
			try {
				int len = channel.write(buffer);
				if(Admin.debug)
				ml.log("[NIOHandler]send a packet:"+buffer+",len:"+len);
			} catch (IOException e) {
				ml.error("send data to server failed:"+e.getMessage());
				buffer.clear();
				break;
			}
		}
	}

	@Override
	public void processError(Exception e) {
		// TODO Auto-generated method stub

	}
	/**
	 * remove all data want to send to this key
	 * @param key
	 */
	public void removeWriteKey(SelectionKey key){
		waitWritePipeLine.removeNode(key);
	}
	private packet receive(SocketChannel channel) throws IOException {
		buffer.clear();
		buffer.limit(Integer.SIZE);
		channel.read(buffer);
		if(buffer.position() == 0){
			System.out.println("no data received");
			return null;
		}
		buffer.flip();
		int msgLen = buffer.getInt();
		System.out.println("[NIOServerHandler]packet len:"+msgLen);
		buffer.clear();
		buffer.limit(msgLen);
		while(buffer.hasRemaining()){
			channel.read(buffer);
		}
		System.out.println("[NIOServerHandler][receive]get buffer:"+buffer);
		packet p = PacketProtocolImpl.CreatePacket(buffer);
		return p;
	}
	public void init(){
		//System.out.println("start NIOServerHandler");
		Thread t = new Thread(){
			public void run(){
				while(true)
						try {
							while(!waitWritePipeLine.hasNext()){
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							processWrite();
						} catch (IOException e) {
							e.printStackTrace();
						}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	@Override
	public void run() {
		//System.out.println("ml is"+(ml == null));
		init();
		//1.open selector
		try {
			selector = Selector.open();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//2.start server
		try {
			serverchannel = startServer(port, selector);
		} catch (ClosedChannelException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		int signals = 0;
		//3.check interest and accept channel;
		while (true) {
			//System.out.println("[NIOServerHandler]check interest!");
			CheckInterest();
			try {
				signals = selector.select(2000);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (signals == 0) {
				continue;
			}
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			SocketChannel channel = null;
			while (it.hasNext()) {
				SelectionKey key = it.next();
				if(!key.isValid()){
					it.remove();
					continue;
				}
				if (key.isValid()&&key.isAcceptable()) {
					System.out.println("[NIOServerHandler]accept a connection");
					try {
						channel = serverchannel.accept();
						ml.log("accept a new connection from "
								+ channel.socket().getInetAddress()
										.getHostAddress());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					SelectionKey thiskey = registerChannel(selector, channel, SelectionKey.OP_READ);
					it.remove();
					//key.interestOps(key.interestOps() & (~key.readyOps()));
					//pushWriteSegement(thiskey,new packet(BasicMessage.NODE,BasicMessage.OP_MESSAGE,"hello world11!"));
					//pushWriteSegement(thiskey,new packet(BasicMessage.NODE,BasicMessage.OP_MESSAGE,"hello world22!"));
					continue;
				}
				it.remove();
                // The key indexes into the selector so you  
                // can retrieve the socket that's ready for I/O  
                execute(key); 
			}

		}
	}
	private void execute(SelectionKey key) {
		if(key.isReadable()){
//			System.out.println("[NIOServerhandler]isreadable!!!!!");
			inprocesskey = key;
			try {
				processRead();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private SelectionKey registerChannel(Selector selector, SocketChannel channel,
			int opRead) {

		if (channel == null) {
			return null;
		}
		try {
			channel.configureBlocking(false);
			return channel.register(selector, opRead);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}
	/**
	 * check all the registered need read key and need cancel key;
	 */
	private void CheckInterest() {
//		while (true) {
//			SelectionKey key = manager.popNeedReadKey();
//			if (key != null && key.isValid()) {
//				key.interestOps(key.interestOps() & (~key.readyOps()));
//				key.interestOps(SelectionKey.OP_READ);
//			} else {
//				break;
//			}
//		}
		while (true) {
			SelectionKey key = manager.popNeedCancelKey();
			if (key != null) {
				ml.log("add node from "
						+ ((SocketChannel) key.channel()).socket()
								.getInetAddress().getHostAddress()+" to delete node");
				manager.deletenode(key);
				key.cancel();
			} else {
				break;
			}
		}
	}
	private ServerSocketChannel startServer(int port, Selector selector) throws IOException,
	ClosedChannelException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.bind(new InetSocketAddress(port));
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		return serverChannel;
}
	public segment getNewSegement() {
		return pipeline.popNode();
	}
	public packet getChannelRecv(SelectionKey key){
		//System.out.println("key is :"+key.toString());
		//System.out.println("pipeline is null?"+(pipeline == null));
		return pipeline.popNode(key);
	}
	public void pushWriteSegement(SelectionKey key,packet p){
		//System.out.println("[NIOServerHandler]pushWriteSegment");
		while(!waitWritePipeLine.addNode(key, p)){
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public SelectionKeyManager getNodeList(){
		return manager;
	}
	public void flush(){
		try {
			processWrite();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void close(){
		try {
			selector.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
