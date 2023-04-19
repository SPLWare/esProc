package com.scudata.server.unit;

import java.util.List;

import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.server.ConnectionProxyManager;
import com.scudata.server.IProxy;
import com.scudata.util.DatabaseUtil;

/**
 * 连接代理对象
 * 
 * @author Joancy
 *
 */
public class ConnectionProxy extends IProxy
{
	Context context;
	String spaceId;
	boolean closed = false;

	/**
	 * 创建一个连接代理
	 * @param cpm 连接代理管理器
	 * @param id 代理编号
	 * @param spaceId 空间编号
	 */
	public ConnectionProxy(ConnectionProxyManager cpm, int id, String spaceId){
		super(cpm, id);
		this.spaceId = spaceId;
		context = new Context();
		List connectedDsNames = null;
		UnitServer us = UnitServer.instance;
		if( us != null ){
			if(us.getRaqsoftConfig()!=null){
				connectedDsNames = us.getRaqsoftConfig().getAutoConnectList();
			}
		}
		DatabaseUtil.connectAutoDBs(context, connectedDsNames);

		JobSpace js = JobSpaceManager.getSpace(spaceId);
		context.setJobSpace(js);
		access();
		Logger.debug(this+" connected.");
	}
	
	/**
	 * 根据id获取Statement代理器
	 * @param id 代理编号
	 * @return Statement代理器
	 * @throws Exception
	 */
	public StatementProxy getStatementProxy(int id) throws Exception{
		StatementProxy sp = (StatementProxy)getProxy(id); 
		if(sp==null){
			throw new Exception("Statement "+id+" is not exist or out of time!");
		}
		return sp; 
	}
	
	/**
	 * 获取计算环境上下文
	 * @return 环境上下文
	 */
	public Context getContext(){
		return context;
	}
	/**
	 * 获取任务空间号
	 * @return 空间编号
	 */
	public String getSpaceId(){
		return spaceId;
	}
	
	/**
	 * 判断连接是否已关闭
	 * @return 关闭返回true，否则返回false
	 */
	public boolean isClosed(){
		return closed;
	}
	
	/**
	 * 关掉当前连接代理器
	 */
	public void close() {
		JobSpaceManager.closeSpace(spaceId);
		DatabaseUtil.closeAutoDBs(context);
		closed =  true;
		Logger.debug(this+" closed.");
	}

	/**
	 * 实现toString接口
	 */
	public String toString() {
		return "Connection "+getId();
	}
	
}