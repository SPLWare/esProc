package com.raqsoft.parallel;

/**
 * 多播监听器接口
 * @author Joancy
 *
 */
public interface MulticastListener{
	/**
	 * 往监听器里面增加一个节点机地址
	 * @param host 主机IP地址
	 * @param port 主机端口号
	 */
  public void addUnitClient(String host, int port);
}
