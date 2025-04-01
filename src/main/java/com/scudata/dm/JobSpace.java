package com.scudata.dm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.expression.DfxFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.FunctionLib;
import com.scudata.parallel.UnitClient;
import com.scudata.resources.EngineMessage;

/**
 * 任务空间
 * @author RunQian
 *
 */
public class JobSpace {
	private String id;
	private ParamList paramList = new ParamList(); // 存放全程变量
	private long lastAccess = System.currentTimeMillis(); // 最后访问时间
	private File appHome = null;

	private ArrayList<UnitClient> unitClients = new ArrayList<UnitClient>();
	private ResourceManager rm = new ResourceManager();

	// 程序网格函数映射表，[函数名,程序网路径名]
	private HashMap<String, DfxFunction> dfxFnMap = new HashMap<String, DfxFunction>(256);
	
	 public JobSpace(String ID) {
		this.id = ID;
	}

	public String toString(){
		return "JobSpace "+id;
	}
	
	public String description(){
		StringBuffer sb = new StringBuffer();
		sb.append("[ "+toString());
		if(paramList.count()>0){
			sb.append(" [Params:");
			int n = paramList.count();
			for(int i=0;i<n;i++){
				if(i>0){
					sb.append(",");
				}
				sb.append(paramList.get(i).getName());
			}
			sb.append(" ]");
		}
		if(appHome!=null){
			sb.append(" App home:");
			sb.append( appHome );
		}
		if(unitClients.size()>0){
			sb.append(" [Callx nodes:");
			int n = unitClients.size();
			for(int i=0;i<n;i++){
				if(i>0){
					sb.append(",");
				}
				sb.append(unitClients.get(i));
			}
			sb.append(" ]");
		}
		
		sb.append(" ]");
		return sb.toString();
	}
	
	public String getID() {
		lastAccess = System.currentTimeMillis();
		return id;
	}

	/**
	 * 取所有变量
	 * 
	 * @return Param[]
	 */
	public Param[] getAllParams() {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();

			int size = paramList.count();
			Param[] params = new Param[size];
			for (int i = 0; i < size; ++i) {
				params[i] = paramList.get(i);
			}

			return params;
		}
	}

	/**
	 * 按名称取变量
	 * 
	 * @param name 变量名
	 * @return DataStruct
	 */
	public Param getParam(String name) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			return paramList.get(name);
		}
	}

	/**
	 * 添加变量
	 * 
	 * @param param 变量
	 */
	public void addParam(Param param) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			paramList.add(param);
		}
	}

	/**
	 * 按名称删除变量
	 * 
	 * @param name String
	 * @return Param
	 */
	public Param removeParam(String name) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			return paramList.remove(name);
		}
	}

	/**
	 * 删除所有变量
	 */
	public void clearParam() {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			paramList.clear();
		}
	}

	/**
	 * 设置变量的值，如果变量不存在则产生一个
	 * 
	 * @param name String 变量名
	 * @param value Object 变量值
	 */
	public void setParamValue(String name, Object value) {
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();

			Param p = paramList.get(name);
			if (p == null) {
				paramList.add(new Param(name, Param.VAR, value));
			} else {
				p.setValue(value);
			}
		}
	}
	
	// 先锁住变量再计算x，为了支持同步做env(v,v+n)
	public Object setParamValue(String name, Expression x, Context ctx) {
		Param p;
		synchronized (paramList) {
			lastAccess = System.currentTimeMillis();
			p = paramList.get(name);
			if (p == null) {
				p = new Param(name, Param.VAR, null);
				paramList.add(p);
			}
		}
		
		synchronized(p) {
			Object value = x.calculate(ctx);
			p.setValue(value);
			return value;
		}
	}

	/**
	 * 返回最后访问时间
	 * 
	 * @return long
	 */
	public long getLastAccessTime() {
		return lastAccess;
	}

	public void addHosts(String host, int port) { // callx分配任务到指定host后，将host增加hosts，并更新访问时间
		UnitClient uc = new UnitClient(host, port);
		synchronized (unitClients) {
			if (!unitClients.contains(uc)) {
				unitClients.add(uc);
			}
		}
	}
	
	public void close() {
		close(true);
	}
	
	public void closeResource(){
		close(false);
	}
	
	private void close(boolean paramCleared){
		//主程序如果调用了callx(h)，需要将分配过的h记在space，用于此处通知分机关闭空间
		//分机主进程调用分进程产生的空间不记载分机Units信息，由主进程在关闭空间时，从HostManager找出自己的units再关闭		
		for (int i = 0; i < unitClients.size(); i++) {
			UnitClient uc = unitClients.get(i);
			uc.closeSpace(id);
		}
		
		if(paramCleared) paramList.clear();
		rm.close();
		DfxManager.getInstance().clear();
	}

	public boolean checkTimeOut(int timeOut) {
		// 换算成秒，timeOut单位为秒
		long unvisit = (System.currentTimeMillis() - lastAccess) / 1000;
		if (unvisit > timeOut) {
			// destroy();
			return true;
		}
		return false;
	}

	/**
	 * 取资源管理器
	 * @return
	 */
	public ResourceManager getResourceManager() {
		return rm;
	}
	
	/** 设应用主目录 */
	public void setAppHome(File f){
		this.appHome = f;
	}
	/** 取应用主目录 */
	public File getAppHome() {
		return this.appHome;
	}
	/** 取应用程序目录 */
	public File getAppProgPath() {
		return new File(this.appHome, "prog");
	}
	
	/**
	 * 添加程序网函数
	 * @param fnName 函数名
	 * @param dfxPathName 程序网路径名
	 */
	public void addDFXFunction(String fnName, String dfxPathName, String opt) {
		// 不能与全局函数重名
		if (FunctionLib.isFnName(fnName)) {
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(mm.getMessage("FunctionLib.repeatedFunction") + fnName);
		}

		// 用新函数替换旧的
		DfxFunction old = dfxFnMap.put(fnName, new DfxFunction(dfxPathName, opt));
		if (old != null) {
			// 清除缓存
			DfxManager.getInstance().clearDfx(dfxPathName);
		}
	}

	/**
	 * 添加程序网函数
	 * @param fnName 函数名
	 * @param funcInfo 函数体信息
	 */
	public void addDFXFunction(String fnName, PgmCellSet.FuncInfo funcInfo) {
		// 不能与全局函数重名
		if (FunctionLib.isFnName(fnName)) {
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(mm.getMessage("FunctionLib.repeatedFunction") + fnName);
		}
		
		dfxFnMap.put(fnName, new DfxFunction(funcInfo));
	}
	
	/**
	 * 删除程序网函数
	 * @param fnName 函数名
	 */
	public void removeDFXFunction(String fnName) {
		dfxFnMap.remove(fnName);
	}

	/**
	 * 根据函数名取程序网
	 * @param fnName 函数名
	 * @return 程序网函数
	 */
	public DfxFunction getDFXFunction(String fnName) {
		return dfxFnMap.get(fnName);
	}
}
