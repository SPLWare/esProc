package com.raqsoft.parallel;

import java.net.*;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.resources.ParallelMessage;

/**
 * 多播监控器，利用组播在局域网内应答
 * 管理器的分机搜索功能
 * @author Joancy
 *
 */
public class MulticastMonitor extends Thread {
	public static String MULTICAST_HOST = "231.0.0.1";
	public static int MULTICAST_PORT = 18281;
	public static int MULTICAST_PORT2 = 18282;
	
//广播的消息一类用于搜索节点机，使用REPLY_LIVENODES作为前缀应答
//其余的为服务端给客户监听发送的普通消息	
	public static String LIST_LIVENODES = "List live nodes";
	public static String REPLY_LIVENODES="REPLY_LIVENODES";

	private String host = null;
	private int port = -1;

	private volatile boolean stop = false;
	private MulticastListener listener;

	private boolean isServer = false;
	static MessageManager mm = ParallelMessage.get();
	final static boolean localDebug = false;
	
	/**
	 * 获取当前多播宿主机的IP描述信息
	 */
	public String toString(){
		return "["+host+":"+port+"]";
	}

	/**
	 * 集群管理的多播，使用另一套地址
	 */
	public void setClusterManager(){
		MULTICAST_HOST = "231.0.0.2";
		MULTICAST_PORT = 18381;
		MULTICAST_PORT2 = 18382;
	}
	
	/**
	 * 设置当前宿主机监听的IP跟端口信息
	 * @param host IP地址
	 * @param port 端口号
	 */
	public void setHost(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 停止当前多播服务
	 */
	public void stopThread() {
		stop = true;
	}

	/**
	 * 构造一个多播监控线程,调用该缺省参数的
	 * 构造函数表示该多播服务为服务端
	 */
	public MulticastMonitor() {
		isServer = true;
	}
	
	/**
	 * 使用多播监控器构造一个多播监控线程
	 * 此时的多播监控器为客户端
	 * @param listener 多播监控器
	 */
	public MulticastMonitor(MulticastListener listener) {
		this.listener = listener;
		this.setName(toString());
	}

	/**
	 * 往局域网内发送广播消息，寻找活动的节点机
	 */
	public void broadcast() {
		send(LIST_LIVENODES);
	}

	private void send(String message) {
		try {
			int multicastPort = MULTICAST_PORT;
			if (isServer) {
				multicastPort = MULTICAST_PORT2;
			}
			InetAddress ip = InetAddress.getByName(MULTICAST_HOST);
			MulticastSocket ms = new MulticastSocket();
			ms.joinGroup(ip);
			DatagramPacket packet = new DatagramPacket(message.getBytes(),
					message.length(), ip, multicastPort);
			ms.send(packet);
			Logger.debug("Use port:"+multicastPort+" broad a message："+message);
			ms.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handle(DatagramPacket packet) {
		try {
			String message = new String(packet.getData(), 0, packet.getLength());
			Logger.debug("Receive message："+message);
			if (message.equals(LIST_LIVENODES)) {
				if (host == null) {
					return;
				}
				send(REPLY_LIVENODES+host + ":" + port);
				return;
			}
			else{
//				只有客户端才有listener
				if (listener == null) {
					return;
				}
				if(message.startsWith(REPLY_LIVENODES)){
					message = message.substring(REPLY_LIVENODES.length());
					String unitHost = UnitClient.parseHost(message);
					if (unitHost == null) {
						return;
					}
					int unitPort = UnitClient.parsePort(message);
					listener.addUnitClient(unitHost, unitPort);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	boolean isRunning = false;
	boolean isRunning(){
		return isRunning;
	}
	
	public void run() {
		try {
			int multicastPort = MULTICAST_PORT2;
			String thisHost = UnitContext.getDefaultHost();
			String msg = "Host： "+ thisHost;
			int waitTime = 1000;
			if (isServer) {
				waitTime = 3000;
				multicastPort = MULTICAST_PORT;
			}

			MulticastSocket ms = new MulticastSocket(multicastPort);
			ms.setTrafficClass(0x04);
			ms.setSoTimeout( waitTime );
			InetAddress ip = InetAddress.getByName(MULTICAST_HOST);
			ms.joinGroup(ip);
			Logger.debug(msg + " has started multicast listening.");
			while (!stop) {
				isRunning = true;
				try{
					byte[] data = new byte[256];
					DatagramPacket packet = new DatagramPacket(data, data.length);
					ms.receive(packet);
					handle( packet );
				} catch (java.net.SocketTimeoutException ste) {
				}
			}
			ms.close();
			Logger.debug(msg+ " halt multicast listening.");
		} catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	
}
