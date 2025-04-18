package com.scudata.parallel;

import java.util.HashMap;
import java.util.Map;

import com.scudata.common.MessageManager;
import com.scudata.dm.ZoneManager;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.unit.UnitServer;

/**
 * 节点机作业线程
 * 
 * @author Joancy
 *
 */
public class UnitWorker extends Thread {
	UnitServer us;
	SocketData socketData;
	
//	来自于socket的客户端是否通过了白名单验证
	boolean errorCheck = false;
	String clientIP=null;
	
	private volatile boolean stop = false;

	/**
	 * 构造一个作业线程
	 * @param tg 线程组
	 * @param name 线程名称
	 */
	public UnitWorker(ThreadGroup tg, String name){
		super(tg,name);
	}
	
	public void setErrorCheck(String clientIP){
		errorCheck = true;
		this.clientIP = clientIP;
	}
	
	/**
	 * 设置数据套接字通讯对象
	 * @param sd 数据套接字
	 */
	public void setSocket(SocketData sd,UnitServer currentServer){
		this.socketData = sd;
		this.us = currentServer;
	}

	/**
	 * 线程运行
	 */
	public void run() {
		try {
			Response response = null;
			while (!stop) {
				Object obj = socketData.read();

				if (obj == null || !(obj instanceof Request)) {
					break;
				}
				Request req = (Request) obj;
				switch (req.getActionType()) {
				case Request.TYPE_DFX:
					setName("UnitWorker[execute splx]:"+req);
					if (req.getAction()==Request.TYPE_DFX+1){
						doTracer("splx", req);
					}
					if(errorCheck){
						response = new Response();
						MessageManager mm = ParallelMessage.get();
						Exception error = new Exception(mm.getMessage("UnitWorker.errorcheck",clientIP));
						response.setException(error);
						break;
					}else{
						response = TaskManager.execute(req);
					}
					break;
				case Request.TYPE_CURSOR:
					setName("UnitWorker[serve cursor]:"+req);
					int taskId = ((Number) req.getAttr(Request.METHOD_TaskId)).intValue();
					try {
						RemoteCursorProxyManager rcpm;
						if(taskId==-1){
//							集群游标没有任务号，使用静态的游标管理器
							rcpm = RemoteCursorProxyManager.getInstance();
						}else{
							ITask t = TaskManager.getTask(taskId);
							rcpm = t.getCursorManager();
						}
						response = rcpm.execute(req);
					} catch (Exception x) {
						response = new Response();
						response.setException(x);
					}
					break;
				case Request.TYPE_FILE:
					setName("UnitWorker[serve file]:"+req);
					//为了提高读取文件的速度不能在读文件的过程中采用request,response的问答式，而是需要
					//对socketData连续写文件数据，所以这类服务需要把socketData交给相应管理器，如下分区partition服务同。
					response = RemoteFileProxyManager.execute(req, socketData);
					break;
				case Request.TYPE_PARTITION:
					setName("UnitWorker[serve partition]:"+req);
					response = PartitionManager.execute(req, socketData);
					break;
				case Request.TYPE_ZONE:
					setName("UnitWorker[ZONE]:"+req);
					response = ZoneManager.execute(req);
					break;
				case Request.TYPE_UNITCOMMAND:
					setName("UnitWorker[UnitCommand]:"+req);
					UnitCommand uc = (UnitCommand)req.getAttr(Request.EXE_Object);
					response = uc.execute();
					break;
				case Request.TYPE_JDBC:
					setName("UnitWorker[JDBC]:"+req);
					response = com.scudata.server.unit.JdbcManager.execute(req);
					break;
				default: // Type Server
					setName("UnitWorker[execute cmd]:"+req);
					response = us.execute(req);
				}
				socketData.write(response);
			}
		} catch (Exception x) {
			x.printStackTrace();
		} finally {
			try {
				socketData.serverClose();
			} catch (Exception x) {
			}
		}
	}

	/**
	 * 停止线程作业
	 */
	public void shutdown() {
		stop = true;
	}

	/**
	 * 实现toString描述，方便调试查看线程信息
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("UnitWorker:");
		sb.append(socketData.getSocket().getRemoteSocketAddress());
		sb.append("-"+socketData.getSocket().hashCode());
		return sb.toString();
	}

	// otel collect data by agent
	private void doTracer(String typeName, Request req){
		Map<String, Object>map = new HashMap<>();
		map.put("fileName", req.getAttr("Dfx name"));
		map.put("params", req.getAttr("Arg list"));
		
		collectData(typeName, map);
	}
	
	public void collectData(String typeName, Map<String, Object>map){	
	}

}
