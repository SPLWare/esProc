package com.scudata.dm.op;

import com.scudata.array.IntArray;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.GroupsSyncReader;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;

/**
 * 内存分组汇总结果基类
 * @author RunQian
 *
 */
abstract public class IGroupsResult implements IResult {
	/**
	 * 根据参数取内存分组汇总处理类
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param srcDs 源数据结构，用于把#n变成相应字段名
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return IGroupsResult
	 */
	public static IGroupsResult instance(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, DataStruct srcDs, String opt, Context ctx) {
		if (exps != null) {
			int count = exps.length;
			if (names == null) {
				names = new String[count];
			}
			
			for (int i = 0; i < count; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					names[i] = exps[i].getFieldName(srcDs);
				}
			}
		}

		if (calcExps != null) {			
			int valCount = calcExps.length;
			if (calcNames == null) {
				calcNames = new String[valCount];
			}
			
			for (int i = 0; i < valCount; ++i) {
				if (calcNames[i] == null || calcNames[i].length() == 0) {
					calcNames[i] = calcExps[i].getFieldName(srcDs);
				}
			}
		}
		
		boolean XOpt = false;
		if (opt != null && opt.indexOf('X') != -1)
			XOpt = true;
		if (exps != null && exps.length == 1 && !XOpt) {
			String gname = names == null ? null : names[0];
			return new Groups1Result(exps[0], gname, calcExps, calcNames, opt, ctx);
		} else {
			return new GroupsResult(exps, names, calcExps, calcNames, opt, ctx);
		}
	}
	
	/**
	 * 根据参数取内存分组汇总处理类
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param srcDs 源数据结构，用于把#n变成相应字段名
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param capacity 用于分组运算的哈希表容量
	 * @return IGroupsResult
	 */
	public static IGroupsResult instance(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, DataStruct srcDs, String opt, Context ctx, int capacity) {
		if (exps != null) {
			int count = exps.length;
			if (names == null) {
				names = new String[count];
			}
			
			for (int i = 0; i < count; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					names[i] = exps[i].getFieldName(srcDs);
				}
			}
		}

		if (calcExps != null) {			
			int valCount = calcExps.length;
			if (calcNames == null) {
				calcNames = new String[valCount];
			}
			
			for (int i = 0; i < valCount; ++i) {
				if (calcNames[i] == null || calcNames[i].length() == 0) {
					calcNames[i] = calcExps[i].getFieldName(srcDs);
				}
			}
		}
		
		if (exps != null && exps.length == 1) {
			String gname = names == null ? null : names[0];
			return new Groups1Result(exps[0], gname, calcExps, calcNames, opt, ctx, capacity);
		} else {
			return new GroupsResult(exps, names, calcExps, calcNames, opt, ctx, capacity);
		}
	}
	
	/**
	 * 取分组表达式
	 * @return 表达式数组
	 */
	abstract public Expression[] getExps();

	/**
	 * 取分组字段名
	 * @return 字段名数组
	 */
	abstract public String[] getNames();

	/**
	 * 取汇总表达式
	 * @return 表达式数组
	 */
	abstract public Expression[] getCalcExps();

	/**
	 * 取汇总字段名
	 * @return 字段名数组
	 */
	abstract public String[] getCalcNames();

	/**
	 * 取选项
	 * @return
	 */
	abstract public String getOption();
	
	/**
	 * 取是否是有序分组
	 * @return true：是，数据按分组字段有序，false：不是
	 */
	abstract public boolean isSortedGroup();

	/**
	 * 取二次汇总表达式，用于多线程分组
	 * @return
	 */
	abstract public Expression[] getRegatherExpressions();
	
	/**
	 * 取二次汇总数据结构
	 * @return DataStruct
	 */
	abstract public DataStruct getRegatherDataStruct();
	
	/**
	 * 取二次汇总后用于计算最终结果的表达式，avg可能被分成sum、count两列进行计算
	 * @return
	 */
	abstract public Expression[] getResultExpressions();

	/**
	 * 取结果集数据结构
	 * @return DataStruct
	 */
	abstract public DataStruct getResultDataStruct();
	
	/**
	 * 并行运算时，取出每个线程的中间计算结果，还需要进行二次汇总
	 * @return Table
	 */
	abstract public Table getTempResult();
	
	/**
	 * 取分组汇总结果
	 * @return Table
	 */
	abstract public Table getResultTable();
	
	 /**
	  * 数据推送结束，取最终的计算结果
	  * @return
	  */
	public Object result() {
		return getResultTable();
	}
	
	/**
	 * 处理推送过来的数据，累积到最终的结果上
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	abstract public void push(Sequence table, Context ctx);

	/**
	 * 处理推送过来的游标数据，累积到最终的结果上
	 * @param cursor 游标数据
	 */
	abstract public void push(ICursor cursor);

	/**
	 * 处理推送过来的游标数据，累积到最终的结果上
	 * @param reader 游标数据
	 */
	public void push(GroupsSyncReader reader, int hashStart, int hashEnd) {
		throw new RuntimeException();
	}
	
	public void push(Sequence table, Object key, IntArray hashValue, int hashStart, int hashEnd) {
		throw new RuntimeException();
	}
	
	/**
	 * 设置分组数，@z选项使用
	 * @param groupCount
	 */
	public void setCapacity(int capacity) {
	}
	
	/**
	 * 设置分组数，@n选项使用
	 * @param groupCount
	 */
	abstract public void setGroupCount(int groupCount);
	
	/**
	 * 多路运算时对把所有路的运算结果合并进行二次分组汇总，得到最终的汇总结果
	 * @param results 所有路的分组结果构成的数组
	 * @return 最终的汇总结果
	 */
	abstract public Object combineResult(Object []results);
	
	/**
	 * 多路游标并行分组完成后进行合并，生成最终分组结果
	 * @param others
	 * @param ctx 计算上下文
	 * @return
	 */
	public Table combineGroupsResult(IGroupsResult []others, Context ctx) {
		Table result = getTempResult();
		for (IGroupsResult other : others) {
			if (result == null) {
				result = other.getTempResult();
			} else {
				result.addAll(other.getTempResult());
			}
		}
		
		if (result == null || result.length() == 0) {
			return result;
		}
		
		// 生成二次分组汇总表达式，avg可能被分成sum、count两列进行计算
		Expression []valExps = getRegatherExpressions();
		DataStruct tempDs = getRegatherDataStruct();
		int tempFieldCount = tempDs.getFieldCount();
		
		int keyCount;
		if (valExps != null) {
			keyCount = tempFieldCount - valExps.length;
		} else {
			keyCount = tempFieldCount;
		}
		
		// 生成二次分组分组表达式
		Expression []keyExps = null;
		String []names = null;
		if (keyCount > 0) {
			keyExps = new Expression[keyCount];
			names = new String[keyCount];
			for (int i = 0, q = 1; i < keyCount; ++i, ++q) {
				keyExps[i] = new Expression(ctx, "#" + q);
				names[i] = tempDs.getFieldName(i);
			}
		}

		String []calcNames = null;
		if (tempFieldCount > keyCount) {
			int gatherCount = tempFieldCount - keyCount;
			calcNames = new String[gatherCount];
			for (int i = 0; i < gatherCount; ++i) {
				calcNames[i] = tempDs.getFieldName(keyCount + i);
			}
		}
		
		// 进行二次分组
		result = result.groups(keyExps, names, valExps, calcNames, getOption(), ctx);
		Expression []newExps = getResultExpressions();
		if (newExps != null) {
			return result.newTable(getResultDataStruct(), newExps, null, ctx);
		} else {
			return result;
		}
	}
}
