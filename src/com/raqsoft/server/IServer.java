package com.raqsoft.server;

import com.raqsoft.app.config.RaqsoftConfig;

/**
 * 服务器接口
 * 目前实现了三种服务器
 * 1：UnitServer 节点机
 * 2：OdbcServer odbc服务器
 * 3：HttpServer http服务器
 * 
 * @author Joancy
 *
 */
public interface IServer extends Runnable {
	public static long devTimeout = 48*60*60*1000;//两天
	public static long MAX_COUNT = 200;//最多允许跑200次计算
	public static long startTime = System.currentTimeMillis();
	
	/**
	 * 获取当前服务器的运行状态
	 * @return 正在运行返回true，否则返回false
	 */
	public boolean isRunning();
	
	/**
	 * 获取当前服务器能否在打开界面窗口后，自动启动服务
	 * @return 需要自动启动返回true，否则返回false
	 */
	public boolean isAutoStart();
	
	/**
	 * 获取服务器监听IP地址
	 * @return IP地址
	 */
	public String getHost();
	
	/**
	 * 停止服务
	 */
	public void shutDown();
	
	/**
	 * 设置服务器配置信息
	 * @param rc 配置
	 */
	public void setRaqsoftConfig(RaqsoftConfig rc);
	
	/**
	 * 获取配置信息
	 */
	public RaqsoftConfig getRaqsoftConfig();
	
	/**
	 * 设置服务器启动侦听类
	 * @param listen
	 */
	public void setStartUnitListener(StartUnitListener listen);

}
