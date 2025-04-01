package com.scudata.server.unit;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.common.*;
import com.scudata.parallel.*;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.server.StartUnitListener;

import java.text.*;

/**
 * 非图形服务器抽象类
 * 不能调用GM等ide的类， 分机
 * 
 * @author Joancy
 *
 */
public abstract class UnitServer implements IServer {
	public static UnitServer instance;

	protected static boolean isIniting = false;
	static int objectId = 0;
	static Object idLock = new Object();

	
	/**
	 * 设置环境配置
	 */
	public abstract void setRaqsoftConfig(RaqsoftConfig rc);
	/**
	 * 获取环境参数配置对象
	 */
	public abstract RaqsoftConfig getRaqsoftConfig();
	
	/**
	 * 设置服务器状态侦听类
	 */
	public abstract void setStartUnitListener(StartUnitListener listen);
	
	/**
	 * 获取计算环境上下文
	 * @return
	 */
	public abstract UnitContext getUnitContext();

	/**
	 * 分机服务器内部产生唯一编号
	 * 用于代理号等各种不能同号的地方
	 * @return 服务器内唯一编号
	 */
	public static int nextId() {
		synchronized (idLock) {
			int c = ++objectId;
			if (c == Integer.MAX_VALUE) {
				objectId = 1;
				c = 1;
			}
			return c;
		}
	}

	/**
	 * 打出内存占用率，用于调试
	 * @param msg 显示的消息
	 */
	public static void debugMemory(String msg) {
		DecimalFormat df = new DecimalFormat("###,###");
		System.gc();
		long tmp = Runtime.getRuntime().freeMemory();
		String buf = ParallelMessage.get().getMessage("UnitServer.memory",msg,df.format(tmp / 1024));
		Logger.debug( buf );
	}

	/**
	 * 执行分机的请求
	 * @param req 请求对象
	 * @return 响应结果
	 */
	public abstract Response execute(Request req);

	/**
	 * 获取分机唯一实例，从配置文件顺序产生
	 * @return 分机服务器实例
	 * @throws Exception
	 */
	public static UnitServer getInstance() throws Exception {
		throw new RQException("Method not implemented.");
	};
	
	/**
	 * 启动或者获取指定地址的分机
	 * @param specifyHost IP地址
	 * @param specifyPort 端口号
	 * @return 分机实例
	 * @throws Exception
	 */
	public static UnitServer getInstance(String specifyHost, int specifyPort) throws Exception {
		throw new RQException("Method not implemented.");
	}
	
	public static UnitServer getInstance(String host, int port, String cfgPath ) throws Exception {
		throw new RQException("Method not implemented.");
	}

	
	/**
	 * 服务器是否正在初始化环境
	 * @return 初始化时返回true，否则返回false
	 */
	public static boolean isIniting(){
		return isIniting;
	}
	
	/**
	 * 使用参数调用分机的初始化文件init.splx
	 * @param i，第i个分机，i=0表示分机启动时刻的调用
	 * @param N，共N个分机
	 * @param j，任务名称
	 * @return
	 */
	public static Response init(final int i, final int N, final String j){
		throw new RQException("Method not implemented.");
	}
	
	/**
	 * 执行初始化init.dfx脚本
	 * @param z 分机的序号
	 * @param N 总共有几台分机
	 * @param waitResult 是否等待初始化结果，界面控制台不等，非图形控制台等待
	 * 界面控制台如果等待返回结果，则会造成界面被锁死
	 * @return
	 */
	public static Response init(final int i,final int N, final String j, final boolean waitResult){
		throw new RQException("Method not implemented.");
	}
	
	/**
	 * 运行分机，启动服务
	 */
	public abstract void run(); // 启动服务

	/**
	 * 判断当前服务器是否正在运行中
	 */
	public synchronized boolean isRunning() {
		throw new RQException("Method not implemented.");
	}
	
	/**
	 * 退出服务器
	 */
	public abstract void quit();
	
	/**
	 * 停止服务器
	 */
	public abstract void shutDown();

	/**
	 * 获取服务器的home路径
	 * @return home路径
	 */
	public static String getHome() {
		String home = System.getProperty("start.home");
		if(home==null){
			throw new RuntimeException("start.home is not specified!");
		}
		return home;
	}


	/**
	 * 获取当前服务器的Host
	 */
	public abstract String getHost();

	/**
	 * 获取分机IP
	 * @return ip地址
	 */
	public abstract String getIP();
	
	/**
	 * 当前服务器是否是自动启动的
	 */
	public abstract boolean isAutoStart();

}
