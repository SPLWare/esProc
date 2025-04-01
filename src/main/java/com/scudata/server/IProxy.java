package com.scudata.server;

import java.util.ArrayList;

import com.scudata.common.Logger;

/**
 * 代理接口抽象类
 * 
 * @author Joancy
 *
 */
public abstract class IProxy
{
	int id = -1;
	IProxy parent = null;
	public ArrayList<IProxy> subProxies=null;
	long lastAccessTime = -1;
	
	/**
	 * 构造函数
	 * @param parent 父代理
	 * @param id 唯一编号
	 */
	public IProxy(IProxy parent, int id){
		this.parent = parent;
		this.id = id;
	}
	
	/**
	 * 刷新代理的访问时间
	 */
	public void access() {
		lastAccessTime = System.currentTimeMillis();
		if(parent!=null){
			parent.access();
		}
	}

	/**
	 * 重置代理的访问时间
	 */
	public void resetAccess() {
		lastAccessTime = -1;
	}
	
	/**
	 * 销毁代理对象
	 */
	public synchronized void destroy(){
//		先关子代理
		if(subProxies!=null){
			for(int i=0;i<subProxies.size();i++){
				IProxy p = subProxies.get(i);
				p.destroy();
			}
			subProxies.clear();
		}
//		再关自己
		close();
//		从父代理移除自己
		if(parent!=null){
			parent.removeProxy(this);
		}
	}
	
	/**
	 * 追加一个子代理
	 * @param proxy 子代理对象
	 */
	public synchronized void addProxy(IProxy proxy){
		if(subProxies==null){
			subProxies = new ArrayList<IProxy>();
		}
		subProxies.add(proxy);
	}
	
	/**
	 * 移除子代理
	 * @param proxy 子代理对象
	 */
	public synchronized void removeProxy(IProxy proxy){
		if(subProxies!=null){
			subProxies.remove(proxy);
		}
	}

	/**
	 * 根据编号获取子代理
	 * @param id 编号
	 * @return 子代理对象
	 */
	public synchronized IProxy getProxy(int id){
		if(subProxies==null){
			return null;
		}
		for(int i=0;i<subProxies.size(); i++){
			IProxy sub = subProxies.get(i);
			if(sub.getId()==id){
				return sub;
			}
		}
		return null;
	}

	/**
	 * 检查当前代理是否访问超时
	 * @param timeOut 超时时间
	 * @return 超时后销毁对象，返回true，否则返回false
	 */
	public boolean checkTimeOut(int timeOut) {
		if(subProxies!=null){
			for(int i=0;i<subProxies.size(); i++){
				try {
					IProxy sub = subProxies.get(i);
					sub.checkTimeOut(timeOut);
				}catch(Exception x) {
					//超时不再设置同步，避免从上往下，一路锁下来；而直接关代理对象时，会从下往上锁上去，可能死锁
					//这里不同步对象后，可能代理对象已经被关掉，get会越界，对于越界的对象，直接忽略就可以了 xq 2023年12月11日
				}
			}
		}
		if (lastAccessTime < 0) {
			return false; // 还没计算的任务不能检查过期
		}
		// 换算成秒，timeOut单位为秒
		long unvisit = (System.currentTimeMillis() - lastAccessTime) / 1000;
		if (unvisit > timeOut) {
			Logger.debug(this + " is timeout.");
			destroy();
			return true;
		}
		return false;
	}
	
	/**
	 * 获取代理编号
	 * @return 编号
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * 获取父代理
	 * @return 父代理对象
	 */
	public IProxy getParent(){
		return parent;
	}
	
	/**
	 * 获取子代理的总个数
	 * @return 子代理计数
	 */
	public synchronized int size(){
		if(subProxies==null) return 0;
		return subProxies.size();
	}
	
	/**
	 * 关闭当前代理，释放资源
	 */
	public abstract void close();
	
	/**
	 * 获取代理内容描述信息
	 */
	public abstract String toString();
}
