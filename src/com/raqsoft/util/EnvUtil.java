package com.raqsoft.util;

import com.raqsoft.common.ISessionFactory;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;

/**
 * 环境相关的工具类
 * @author RunQian
 *
 */
public class EnvUtil {
	private static final long G = 1024 * 1024 * 1024; // 1G的大小
	private static final int FIELDSIZE = 50; // 估算内存时每个字段占用的空间大小
	private static final int MAXRECORDCOUNT = 20000000; // 内存中保存的最大记录数量
	
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
	 * @return true：可以继续读，false：不可以再读了
	 */
	public static boolean memoryTest(Runtime rt, Sequence table) {
		int len = table.length();
		if (len >= MAXRECORDCOUNT) return false;
		
		long freeMemory = rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
		if (freeMemory < 200000000L) { // 200m
			return false;
		}
		
		int fcount = 1;
		Object obj = table.get(1);
		if (obj instanceof Record) {
			fcount = ((Record)obj).getFieldCount();
		}
		
		int recordCount = (int)G / (fcount * FIELDSIZE); // 1g内存大约能够容纳的记录数
		
		if (freeMemory < 300000000L) { // 300m
			return len < recordCount / 10;
		} else if (freeMemory < 500000000L) { // 500m
			return len < recordCount / 5;
		} else if (freeMemory < 700000000L) { // 700m
			return len < recordCount / 3;
		} else if (freeMemory < G){ // 1g
			return len < recordCount / 2;
		} else {
			return len < recordCount * (freeMemory / G);
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
