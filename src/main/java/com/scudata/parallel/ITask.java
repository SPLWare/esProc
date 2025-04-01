package com.scudata.parallel;

/**
 * 作业接口
 * @author Joancy
 *
 */
public interface ITask{
	/**
	 * 刷新访问时间
	 */
	public void access();
	
	/**
	 * 重置访问时间 
	 */
	public void resetAccess();
	
	/**
	 * 取任务编号
	 * @return 编号
	 */
	public int getTaskID();
	
	 /**
	  * 检查代理超时
	  * @param timeOut 超时时间
	  * @return 正常返回true，否则false
	  */
	public boolean checkTimeOut(int timeOut);
	
	/**
	 * 关闭当前作业
	 */
	public void close();
	
	/**
	 * 取远程游标代理管理器
	 * @return 管理器
	 */
	public RemoteCursorProxyManager getCursorManager();
}
