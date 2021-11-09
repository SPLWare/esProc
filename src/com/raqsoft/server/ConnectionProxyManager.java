package com.raqsoft.server;

import com.raqsoft.server.IProxy;

/**
 * 连接代理管理器
 * 
 * @author Joancy
 *
 */
public class ConnectionProxyManager extends IProxy{
	static ConnectionProxyManager instance = new ConnectionProxyManager();
	
	private ConnectionProxyManager(){
		super(null, 0);
	}
	
	/**
	 * 	覆盖父类该方法，管理器本身不需要过期，维持初始态-1即可
	 */
	public void access() {
	}

	/**
	 * 获取管理器的唯一实例
	 * @return 管理器实例
	 */
	public static ConnectionProxyManager getInstance(){
		return instance;
	}
	
	/**
	 * 根据编号获得代理对象
	 * @param id 编号
	 * @return 代理对象
	 * @throws Exception 没找到对应代理时抛出异常
	 */
	public IProxy getConnectionProxy(int id) throws Exception{
		IProxy cp = getProxy(id);
		if(cp==null){
			throw new Exception("Connection "+id+" is not exist or out of time!");
		}
		return cp;
	}
	
	/**
	 * 实现代理接口，管理器的该方法无意义
	 */
	public void close() {
	}

	/**
	 * 获取文本描述
	 * @return 描述文本
	 */
	public String toString() {
		return "ConnectionProxyManager";
	}
	
	
}