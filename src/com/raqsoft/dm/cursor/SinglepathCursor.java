package com.raqsoft.dm.cursor;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;

/**
 * 用于把多路游标变成单路游标
 * 封装后cs.groups这类运算不会再采用多线程运算
 * @author RunQian
 *
 */
public class SinglepathCursor extends ICursor {
	private ICursor cursor; // 多路游标

	public SinglepathCursor(ICursor cursor) {
		this.cursor = cursor;
		setDataStruct(cursor.getDataStruct());
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			cursor.resetContext(ctx);
			super.resetContext(ctx);
		}
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		return cursor.fetch(n);
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		return cursor.skip();
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		cursor.close();
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		return cursor.reset();
	}
}
