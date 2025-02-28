package com.scudata.dm;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.expression.Expression;

/**
 * dfx缓存管理器
 */
public class DfxManager {
	private static DfxManager dfxManager = new DfxManager();
	private HashMap<String, SoftReference<PgmCellSet>> dfxRefMap = 
		new HashMap<String, SoftReference<PgmCellSet>>();

	private HashMap<String, SoftReference<List<Expression>>> expListMap = 
			new HashMap<String, SoftReference<List<Expression>>>();
	
	private DfxManager() {}

	/**
	 * 取dfx缓存管理器实例
	 * @return DfxManager
	 */
	public static DfxManager getInstance() {
		return dfxManager;
	}

	/**
	 * 清除缓存的程序网
	 */
	public void clear() {
		synchronized(dfxRefMap) {
			dfxRefMap.clear();
		}
	}
	
	public void clearDfx(String name) {
		synchronized(dfxRefMap) {
			File file = new File(name);
			name = file.getPath();
			dfxRefMap.remove(name);
		}
	}
	
	/**
	 * 使用完dfx，还给缓存管理器
	 * @param dfx PgmCellSet
	 */
	public void putDfx(PgmCellSet dfx) {
		Context dfxCtx = dfx.getContext();
		dfxCtx.setParent(null);
		dfxCtx.setJobSpace(null);
		dfx.reset();

		synchronized(dfxRefMap) {
			dfxRefMap.put(dfx.getName(), new SoftReference<PgmCellSet>(dfx));
		}
	}

	/**
	 * 从缓存管理器中取dfx，使用完后需要调用putDfx还给缓存管理器
	 * @param name dfx文件名
	 * @param ctx 计算上下文
	 * @return PgmCellSet
	 */
	public PgmCellSet removeDfx(String name, Context ctx) {
		File file = new File(name);
		name = file.getPath();
		PgmCellSet dfx = null;
		
		synchronized(dfxRefMap) {
			SoftReference<PgmCellSet> sr = dfxRefMap.remove(name);
			if (sr != null) dfx = (PgmCellSet)sr.get();
		}

		if (dfx == null) {
			return readDfx(name, ctx);
		} else {
			// 不再共享ctx中的变量
			Context dfxCtx = dfx.getContext();
			dfxCtx.setEnv(ctx);
			return dfx;
		}
	}

	/**
	 * 从缓存管理器中取dfx，使用完后需要调用putDfx还给缓存管理器
	 * @param fo dfx文件对象
	 * @param ctx 计算上下文
	 * @return PgmCellSet
	 */
	public PgmCellSet removeDfx(FileObject fo, Context ctx) {
		PgmCellSet dfx = null;
		File file = new File(fo.getFileName());
		String name = file.getPath();
		
		synchronized(dfxRefMap) {
			SoftReference<PgmCellSet> sr = dfxRefMap.remove(name);
			if (sr != null) dfx = (PgmCellSet)sr.get();
		}
		
		if (dfx == null) {
			return readDfx(fo, ctx);
		} else {
			// 不再共享ctx中的变量
			Context dfxCtx = dfx.getContext();
			dfxCtx.setEnv(ctx);
			return dfx;
		}
	}
	
	/**
	 * 读取dfx，不会使用缓存
	 * @param fo dfx文件对象
	 * @param ctx 计算上下文
	 * @return PgmCellSet
	 */
	public static PgmCellSet readDfx(FileObject fo, Context ctx) {
		PgmCellSet dfx = fo.readPgmCellSet();
		dfx.resetParam();
		
		// 不再共享ctx中的变量
		Context dfxCtx = dfx.getContext();
		dfxCtx.setEnv(ctx);
		return dfx;
	}
	
	/**
	 * 读取dfx，不会使用缓存
	 * @param name dfx文件名
	 * @param ctx 计算上下文
	 * @return PgmCellSet
	 */
	public static PgmCellSet readDfx(String name, Context ctx) {
		return readDfx(new FileObject(name, null, "s", ctx), ctx);
	}
	
	/**
	 * 取缓存的表达式，表达式计算完后需要调用putExpression方法归还缓存
	 * @param strExp 表达式串
	 * @param ctx 计算上下文
	 * @return Expression
	 */
	public Expression getExpression(String strExp, Context ctx) {
		synchronized(expListMap) {
			SoftReference<List<Expression>> ref = expListMap.get(strExp);
			if (ref != null) {
				List<Expression> expList = ref.get();
				if (expList != null && expList.size() > 0) {
					Expression exp = expList.remove(expList.size() - 1);
					exp.reset();
					return exp;
				}
			}
		}
		
		return new Expression(ctx, strExp);
	}
	
	/**
	 * 表达式计算完成后把表达式缓存起来
	 * @param strExp 表达式串
	 * @param exp 表达式
	 */
	public void putExpression(String strExp, Expression exp) {
		synchronized(expListMap) {
			SoftReference<List<Expression>> ref = expListMap.get(strExp);
			if (ref == null) {
				List<Expression> expList = new ArrayList<Expression>();
				expList.add(exp);
				ref = new SoftReference<List<Expression>>(expList);
				expListMap.put(strExp, ref);
			} else {
				List<Expression> expList = ref.get();
				if (expList == null) {
					expList = new ArrayList<Expression>();
					expList.add(exp);
					ref = new SoftReference<List<Expression>>(expList);
					expListMap.put(strExp, ref);
				} else {
					expList.add(exp);
				}
			}
		}
	}
}
