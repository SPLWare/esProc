package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;

/**
 * 用于对游标做同步取数操作的游标
 * @author RunQian
 *
 */
public class SyncCursor extends ICursor {
	private ICursor cursor;
	
	/**
	 * 创建同步取数游标
	 * @param cursor
	 */
	public SyncCursor(ICursor cursor) {
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
		return cursor.skip(n);
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		return cursor.reset();
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		cursor.close();
	}
}