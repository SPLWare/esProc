package com.scudata.parallel;

import java.util.*;

import com.scudata.common.*;
import com.scudata.dm.cursor.ICursor;

import java.lang.reflect.*;

/**
 * 远程游标代理管理器
 * @author Joancy
 *
 */
public class RemoteCursorProxyManager {
	private static RemoteCursorProxyManager instance = null;
	
	ITask task;
	ArrayList<RemoteCursorProxy> proxys = new ArrayList<RemoteCursorProxy>();

	/**
	 * 为了支持集群游标，使用静态管理器，该管理器不属于任何Task，task为null
	 * @return 游标代理管理器
	 */
	public static RemoteCursorProxyManager getInstance(){
		if(instance==null){
			instance = new RemoteCursorProxyManager(null);
		}
		return instance;
	}
	
	/**
	 * 增加一个游标，返回游标代理号
	 * @param c 游标
	 * @return 代理号
	 */
	public static int addCursor(ICursor c){
		RemoteCursorProxyManager rcpm = RemoteCursorProxyManager.getInstance();
		RemoteCursorProxy rcp = new RemoteCursorProxy(c);
		rcpm.addProxy(rcp);
		return rcp.getProxyID();
	}

	/**
	 * 创建游标代理管理器
	 * @param t 隶属的任务
	 */
	public RemoteCursorProxyManager(ITask t) {
		this.task = t;
	}

	/**
	 * 获取代理列表
	 * @return 代理列表
	 */
	public ArrayList<RemoteCursorProxy> getProxys(){
		return proxys;
	}
	
	/**
	 * 销毁当前代理器
	 */
	public void destroy() {
		for (int i = 0; i < proxys.size(); i++) {
			RemoteCursorProxy rcp = proxys.get(i);
			rcp.destroy();
		}
		proxys.clear();
	}

	/**
	 * 获取任务对象
	 * @return 任务
	 */
	public ITask getTask() {
		return task;
	}

	/**
	 * 执行请求
	 * @param req 请求
	 * @return 响应
	 */
	public Response execute(Request req) {
		if(task!=null){
			task.resetAccess();
		}
		Response res = new Response();
		try {
			int proxyId = ((Number) req.getAttr(Request.METHOD_ProxyId))
					.intValue();
			RemoteCursorProxy rcp = getProxy(proxyId);
			String methodName = (String) req.getAttr(Request.METHOD_MethodName);
			if(methodName.equals("close")&&rcp==null){
//由于代理端游标fetch完后，自动close掉了，此时客户端再发来的close，不需处理，已经不存在该代理。				
				return res;
			}
			Object[] args = (Object[]) req.getAttr(Request.METHOD_ArgValues);
			try {
				Object result = invokeMethod(rcp, methodName, args);
				res.setResult(result);
			} catch (Exception x) {
				x.printStackTrace();
				res.setException(new RQException(x.getCause().getMessage(), x
						.getCause()));
			} catch (Error er) {
				er.printStackTrace();
				res.setError(er);
			}
		} finally {
			if(task!=null){
				task.access();
			}
		}
		return res;
	}

	/**
	 * 增加游标代理
	 * @param rcp 游标代理
	 */
	public synchronized void addProxy(RemoteCursorProxy rcp) {
		proxys.add(rcp);
	}

	/**
	 * 删除代理对象
	 * 所有代理都删空后将任务中的程序网释放
	 * @param proxyID 要删除的代理编号
	 */
	public synchronized void delProxy(int proxyID) {
		for (int i = 0; i < proxys.size(); i++) {
			RemoteCursorProxy rcp = proxys.get(i);
			if (rcp.getProxyID() == proxyID) {
				proxys.remove(i);
				break;
			}
		}
		if (proxys.isEmpty()) {
			if(task!=null){
				TaskManager.delTask(task.getTaskID());
			}
		}
	}

	/**
	 * 根据代理编号proxyID去代理对象
	 * @param proxyID 代理编号
	 * @return 游标代理对象
	 */
	public synchronized RemoteCursorProxy getProxy(int proxyID) {
		for (int i = 0; i < proxys.size(); i++) {
			RemoteCursorProxy rcp = proxys.get(i);
			if (rcp.getProxyID() == proxyID) {
				return rcp;
			}
		}
		return null;
	}

	/**
	 * 使用反射执行类的方法
	 * @param owner 主类
	 * @param methodName 方法名
	 * @param args 参数
	 * @return 执行结果
	 * @throws Exception 出错时抛出异常
	 */
	public static Object invokeMethod(Object owner, String methodName,
			Object[] args) throws Exception {
		Class ownerClass = owner.getClass();

		Method[] ms = ownerClass.getMethods();
		for (int i = 0; i < ms.length; i++) {
			Method m = ms[i];
			if (m.getName().equals(methodName) && isArgsMatchMethod(m, args)) {
				return m.invoke(owner, args);
			}
		}
		StringBuffer argNames = new StringBuffer();
		argNames.append("(");
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				argNames.append(",");
			}
			if (args[i] == null) {
				argNames.append("null");
			} else {
				argNames.append(args[i].getClass().getName());
			}
		}
		argNames.append(")");
		throw new Exception(methodName + argNames + " not found.");
	}

	private static boolean isArgsMatchMethod(Method m, Object[] args) {
		if (args == null) {
			args = new Object[] {};
		}
		Class[] mArgs = m.getParameterTypes();
		if (mArgs.length != args.length) {
			return false;
		}
		for (int i = 0; i < args.length; i++) {
			if (!localMatch(mArgs[i], args[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean localMatch(Class c, Object o) {
		// 参数为null时，假设匹配
		if (o == null)
			return true;

		String n1 = c.getName();
		if (n1.equals("boolean")) {
			return o instanceof Boolean;
		}
		if (n1.equals("byte")) {
			return o instanceof Byte;
		}
		if (n1.equals("short")) {
			return o instanceof Short;
		}
		if (n1.equals("int")) {
			return o instanceof Integer;
		}
		if (n1.equals("long")) {
			return o instanceof Long;
		}
		if (n1.equals("float")) {
			return o instanceof Float;
		}
		if (n1.equals("double")) {
			return o instanceof Double;
		}

		return c.isInstance(o);
	}

	/**
	 * 检查代理超时
	 * @param proxyTimeOut 超时时间
	 */
	public static synchronized void checkTimeOut(int proxyTimeOut) {
		RemoteCursorProxyManager instance = RemoteCursorProxyManager.getInstance();
		// 换算成秒，timeOut单位为秒
		ArrayList<RemoteCursorProxy> proxys = instance.getProxys();
		for (int i = proxys.size() - 1; i >= 0; i--) {
			RemoteCursorProxy rcp = proxys.get(i);
			if (rcp.checkTimeOut(proxyTimeOut)) {
				proxys.remove(rcp);
			}
		}
	}
	
}
