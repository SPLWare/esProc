package com.scudata.dm;

/**
 * 资源接口，对于需要关闭的资源需要实现此接口
 * 资源管理器最后会调用close方法以释放资源
 * @author WangXiaoJun
 *
 */
public interface IResource {
	/**
	 * 使用结束，关闭资源
	 */
	void close();
}