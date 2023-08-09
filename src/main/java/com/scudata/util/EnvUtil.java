package com.scudata.util;

import com.scudata.common.ISessionFactory;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.JobSpace;
import com.scudata.dm.Param;
import com.scudata.dm.Sequence;

/**
 * 环境相关的工具类
 * @author RunQian
 *
 */
public class EnvUtil {
	//private static final long G = 1024 * 1024 * 1024; // 1G的大小
	private static final int FIELDSIZE = 50; // 估算内存时每个字段占用的空间大小
	private static final int MAXRECORDCOUNT = 20000000; // 内存中保存的最大记录数量
	private static double MAX_USEDMEMORY_PERCENT = 0.4;
	
	public static void setMaxUsedMemoryPercent(double d) {
		MAX_USEDMEMORY_PERCENT = d;
	}
	
	// 取当前空闲内存可以大约再生成多少条记录
	/**
	 * 取当前空闲内存大约可以存放多少条记录
	 * @param fcount 字段数
	 * @return 记录数量
	 */
	public static int getCapacity(int fcount) {
		Runtime rt = Runtime.getRuntime();
		runGC(rt);
	
		long freeMemory = rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
		long recordCount = freeMemory / fcount / FIELDSIZE / 3;
		if (recordCount > MAXRECORDCOUNT) {
			return MAXRECORDCOUNT;
		} else {
			return (int)recordCount;
		}
	}
	
	/**
	 * 测试是否还有空闲内存继续从游标读数
	 * @param rt Runtime
	 * @param table 已读的数据
	 * @param readSize 每次读的数据占的内存大小（不准确）
	 * @return true：可以继续读，false：不可以再读了
	 */
	public static boolean memoryTest(Runtime rt, Sequence table, long readSize) {
		int len = table.length();
		if (len >= MAXRECORDCOUNT) return false;
		
		long maxUseMemory = (long)(rt.maxMemory() * MAX_USEDMEMORY_PERCENT);
		long usedMemory = rt.totalMemory() - rt.freeMemory();
		
		if (usedMemory > maxUseMemory || usedMemory + readSize > maxUseMemory) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 执行垃圾收集
	 * @param rt Runtime
	 */
	public static void runGC(Runtime rt) {
		for (int i = 0; i < 4; ++i) {
			rt.runFinalization();
			rt.gc();
			Thread.yield();
		}
	}

	/**
	 * 查找变量，先找上下文中的变量，再找Session中的，最后是Env中的全局变量。
	 * @param varName String 变量名
	 * @param ctx Context 计算上下文
	 * @return Param
	 */
	public static Param getParam(String varName, Context ctx) {
		if (ctx != null) {
			Param p = ctx.getParam(varName);
			if (p != null)return p;

			JobSpace js = ctx.getJobSpace();
			if (js != null) {
				p = js.getParam(varName);
				if (p != null)return p;
			}
		}

		return Env.getParam(varName);
	}

	/**
	 * 删除变量
	 * @param varName 变量名
	 * @param ctx 计算上下文
	 * @return Param 删除的变量，没找到则返回空
	 */
	public static Param removeParam(String varName, Context ctx) {
		if (ctx != null) {
			Param p = ctx.removeParam(varName);
			if (p != null) {
				return p;
			}

			JobSpace js = ctx.getJobSpace();
			if (js != null) {
				p = js.removeParam(varName);
				if (p != null) {
					return p;
				}
			}
		}

		return Env.removeParam(varName);
	}

	/**
	 * 取数据库连接工厂
	 * @param dbName 数据库名
	 * @param ctx 计算上下文
	 * @return ISessionFactory
	 */
	public static ISessionFactory getDBSessionFactory(String dbName, Context ctx) {
		if (ctx == null) {
			return Env.getDBSessionFactory(dbName);
		} else {
			ISessionFactory dbsf = ctx.getDBSessionFactory(dbName);
			return dbsf == null ? Env.getDBSessionFactory(dbName) : dbsf;
		}
	}
}
