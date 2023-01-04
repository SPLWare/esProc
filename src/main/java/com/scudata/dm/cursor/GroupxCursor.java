package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;

/**
 * 外存分组游标，对游标数据进行外存分组，然后以游标方式返回
 * 用于哈希法实现外存分组函数cs.groupx()的二次分组，外存分组会把分组字段哈希值相同的组写到同一个临时文件，
 * 然后每个临时文件生成一个此游标，要取此游标数据时则会进行二次外存分组
 * @author RunQian
 *
 */
public class GroupxCursor extends ICursor {
	private ICursor src; // 游标
	private Expression[] exps; // 分组表达式
	private String []names; // 分组字段名
	private Expression[] calcExps; // 汇总表达式
	private String []calcNames; // 汇总字段名
	private String opt; // 选项
	private int capacity; // 内存中保存的最大分组结果数
	
	private ICursor result; // 结果集游标

	/**
	 * 生成外存分组游标
	 * @param cursor 源数据游标
	 * @param exps 分组表达式数组
	 * @param names	分组字段名数组
	 * @param calcExps 汇总表达式	数组
	 * @param calcNames	汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param capacity	内存中保存的最大分组结果数
	 */
	public GroupxCursor(ICursor cursor, Expression[] exps, String []names, 
			Expression[] calcExps, String []calcNames, String opt, Context ctx, int capacity) {
		this.src = cursor;
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.opt = opt;
		this.ctx = ctx;
		this.capacity = capacity;
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			src.resetContext(ctx);
			super.resetContext(ctx);
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (result == null) {
			if (src == null) {
				return null;
			}
			
			result = src.groupx(exps, names, calcExps, calcNames, opt, ctx, capacity);
		}
		
		return result.fetch(n);
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (result == null) {
			if (src == null) {
				return 0;
			}
			
			result = src.groupx(exps, names, calcExps, calcNames, opt, ctx, capacity);
		}
		
		return result.skipOver(n);
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (src != null) {
			src.close();
			src = null;
			
			if (result != null) {
				result.close();
				result = null;
			}
		}
	}
}
