package com.scudata.server.unit;

import java.util.List;

import com.scudata.common.Logger;
import com.scudata.common.UUID;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpaceManager;
import com.scudata.server.ConnectionProxyManager;
import com.scudata.server.IProxy;
import com.scudata.util.DatabaseUtil;

/**
 * 连接代理对象
 * 一个连接对应一个spaceId，statement之间共享spaceId的任务变量
 * statement关闭时，仅释放资源；connection关闭时，才关闭space 2024年9月11日
 * @author Joancy
 *
 */
public class ConnectionProxy extends IProxy
{
	String spaceId;
	Context context;
	boolean closed = false;

	/**
	 * 创建一个连接代理
	 * @param cpm 连接代理管理器
	 * @param id 代理编号
	 * @param spaceId 空间编号
	 */
	public ConnectionProxy(ConnectionProxyManager cpm, int id){
		super(cpm, id);
		context = new Context();
		List<String> connectedDsNames = null;
		UnitServer us = UnitServer.instance;
		if( us != null ){
			if(us.getRaqsoftConfig()!=null){
				connectedDsNames = us.getRaqsoftConfig().getAutoConnectList();
			}
		}
		DatabaseUtil.connectAutoDBs(context, connectedDsNames);
		spaceId = UUID.randomUUID().toString();
		context.setJobSpace(JobSpaceManager.getSpace(spaceId));

		access();
		Logger.debug(this+" connected.");
	}
	
	public String getSpaceId() {
		return spaceId;
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