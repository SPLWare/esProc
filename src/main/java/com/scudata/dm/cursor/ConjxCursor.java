package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.expression.Expression;

/**
 * 把多个游标纵向连成一个游标，取数时会依次遍历每个游标，直到最后一个游标取数结束
 * 结构以第一个游标为准
 * @author 
 *
 */
public class ConjxCursor extends ICursor {
	private ICursor []cursors; // 游标数组
	private int curIndex = 0; // 当前正在读数的游标索引

	/**
	 * 构建纵向连接游标对象
	 * @param cursors
	 */
	public ConjxCursor(ICursor []cursors) {
		this.cursors = cursors;
		setDataStruct(cursors[0].getDataStruct());
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}
			
			super.resetContext(ctx);
		}
	}
	
	/**
	 * 取分组计算对象
	 * @param exps 分组字段表达式数组
	 * @param names 分组字段名数组
	 * @param calcExps 汇总字段表达式数组
	 * @param calcNames 汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @return IGroupsResult
	 */
	public IGroupsResult getGroupsResult(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx) {
		return cursors[0].getGroupsResult(exps, names, calcExps, calcNames, opt, ctx);
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (cursors.length == curIndex || n < 1) return null;
		Sequence table = cursors[curIndex].fetch(n);
		if (table == null || table.length() < n) {
			curIndex++;
			if (curIndex < cursors.length) {
				if (table == null) {
					return get(n);
				} else {
					Sequence rest;
					if (n == MAXSIZE) {
						rest = get(n);
					} else {
						rest = get(n - table.length());
					}
					
					table = append(table, rest);
				}
			}
		}

		return table;
	}
	
	protected Sequence fuzzyGet(int n) {
		if (cursors.length == curIndex || n < 1) return null;
		Sequence table = cursors[curIndex].fuzzyGet(n);
		if (table != null && table.length() > 0) {
			return table;
		}
		
		curIndex++;
		if (curIndex < cursors.length) {
			return fuzzyGet(n);
		} else {
			return null;
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (cursors.length == curIndex || n < 1) return 0;

		long count = cursors[curIndex].skip(n);
		if (count < n) {
			curIndex++;
			if (curIndex < cursors.length) {
				count += skipOver(n - count);
			}
		}

		return count;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		
		for (int i = 0, count = cursors.length; i < count; ++i) {
			cursors[i].close();
		}
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		curIndex = 0;
		for (int i = 0, count = cursors.length; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		return true;
	}
	
	public ICursor[] getCursors() {
		return cursors;
	}
}
