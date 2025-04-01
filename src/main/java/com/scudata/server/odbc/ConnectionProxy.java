package com.scudata.server.odbc;

import com.scudata.common.Logger;
import com.scudata.server.ConnectionProxyManager;
import com.scudata.server.IProxy;

/**
 * 连接代理器
 * 
 * @author Joancy
 *
 */
public class ConnectionProxy extends IProxy
{
	String userName;
	long loginTime = 0;
	boolean closed = false;

	/**
	 * 创建一个连接代理对象
	 * @param cpm 连接代理管理器
	 * @param id 代理唯一号
	 * @param user 登录用户名
	 */
	public ConnectionProxy(ConnectionProxyManager cpm, int id, String user){
		super(cpm, id);
		this.userName = user;
		loginTime = System.currentTimeMillis();
		access();
		Logger.debug(this+" connected.");
	}
	
	/**
	 * 获取Statement代理
	 * @param id 代理编号
	 * @return Statement代理
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
	 * 获取用户名
	 * @return 用户名
	 */
	public String getUserName(){
		return userName;
	}
	
	/**
	 * 获取登录时间
	 * @return 整数表示法的时间
	 */
	public long getLoginTime(){
		return loginTime;
	}
	
	/**
	 * 是否已关闭
	 * @return 已关闭返回true，否则返回false
	 */
	public boolean isClosed(){
		return closed;
	}
	
	/**
	 * 关闭当前代理对象
	 */
	public void close() {
		closed = true;
		Logger.debug(this+" closed.");
	}

	/**
	 * 实现toString接口
	 */
	public String toString() {
		return "Connection "+getId();
	}
	
}