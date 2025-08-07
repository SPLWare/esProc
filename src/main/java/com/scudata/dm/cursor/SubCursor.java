package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;

/**
 * 子游标，用于从给定游标中取指定总数量
 * @author RunQian
 *
 */
public class SubCursor extends ICursor {
	private ICursor cursor; // 源游标
	private int count; // 已取记录数
	private int total; // 需要取的总记录数
	
	/**
	 * 创建子游标
	 * @param cursor 源游标
	 * @param total 要取的数据的总数量
	 */
	public SubCursor(ICursor cursor, int total) {
		this.cursor = cursor;
		this.total = total;
		setDataStruct(cursor.getDataStruct());
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
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
		if (cursor == null || n < 1) return null;

		if (count + n > total) {
			n = total - count;
			if (n == 0) return null;
		}
		
		Sequence seq = cursor.fetch(n);
		if (seq != null) {
			count += seq.length();
		}
		
		return seq;
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (cursor == null || n < 1) return 0;

		if (count > total - n) {
			n = total - count;
			if (n == 0) return 0;
		}
		
		n = cursor.skip(n);
		count += n;
		return n;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursor != null) {
			// 源游标可能还被其它子游标继续使用，不能关闭
			//cursor.close();
			cursor = null;
		}
	}
}
