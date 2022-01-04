package com.scudata.parallel;

import java.io.*;

import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.dm.cursor.ICursor;

/**
 * 远程游标
 * 
 * @author Joancy
 *
 */
public class RemoteCursor extends ICursor implements Serializable {
	private static final long serialVersionUID = 1L;
	String host;
	int port, taskId, proxyId;

	UnitClient unitClient = null;
	boolean isClosed = false;

	/**
	 * 创建一个远程游标
	 * @param host 主机IP
	 * @param port 端口号
	 * @param taskId 任务号
	 * @param proxyId 游标代理号
	 */
	public RemoteCursor(String host, int port, int taskId, int proxyId){
		this.host = host;
		this.port = port;
		this.taskId = taskId;
		this.proxyId = proxyId;
	}
	
	/**
	 * 创建远程游标
	 * @param host 主机IP
	 * @param port 端口号
	 * @param proxyId 游标代理号
	 */
	public RemoteCursor(String host, int port, int proxyId){
		this(host,port,-1,proxyId);
	}

	UnitClient getUnitClient() throws Exception {
		if (unitClient == null) {
			unitClient = new UnitClient(host, port);
			unitClient.connect();
		}
		return unitClient;
	}

	private Object executeMethod(String methodName, Object[] argValues) {
		try {
			UnitClient uc = getUnitClient();
			Request req = new Request(Request.CURSOR_METHOD);
			req.setAttr(Request.METHOD_TaskId, new Integer(taskId));
			req.setAttr(Request.METHOD_ProxyId, new Integer(proxyId));
			req.setAttr(Request.METHOD_MethodName, methodName);
			req.setAttr(Request.METHOD_ArgValues, argValues);

			Response res = uc.send(req);
			if (res.getError() != null) {
				throw res.getError();
			}
			if (res.getException() != null) {
				throw res.getException();
			}
			return res.getResult();
		} catch (Exception x) {
			throw new RQException("Execute 'RemoteCursor' method " + methodName
					+ " failed on " + unitClient + " for " + x.getMessage(), x);
		}
	}

	protected long skipOver(long n) {
		if( isClosed ) return 0;
		Long I = null;
		try {
			I = (Long) executeMethod("skip", new Object[] { new Long(n) });
			return I.longValue();
		} finally {
			if (I != null && I.longValue() < n) {
				close();
			}
		}
	}

	public synchronized void close() {
		if( isClosed ) return;
		try {
			executeMethod("close", null);
		} catch (Exception x) {
		} finally {
			unitClient.close();
			isClosed = true;
		}
	}

	protected Sequence get(int n) {
		if( isClosed ) return null;
		Sequence t = null;
		try {
			t = (Sequence) executeMethod("fetch", new Object[] { new Integer(n) });
			return t;
		} finally {
			if (t == null || t.length() < n) {
				close();
			}
		}
	}
	
	/**
	 * 取游标的数据结构信息
	 * @return 数据结构
	 */
	public DataStruct getDataStruct() {
		if(dataStruct!=null){
			return dataStruct;
		}
		if( isClosed ) return dataStruct;
		
		dataStruct = (DataStruct) executeMethod("getDataStruct", null);
		return dataStruct;
	}
	

	/**
	 * 实现toString的文本描述
	 */
	public String toString() {
		return "RemoteCursor@" + host + ":" + port + " cursorId:" + proxyId;
	}
}
