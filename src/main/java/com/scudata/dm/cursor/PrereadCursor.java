package com.scudata.dm.cursor;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.thread.ThreadPool;

public class PrereadCursor extends ICursor {
	private ICursor cursor;
	private CursorReader reader;

	public PrereadCursor(ICursor cursor) {
		this.cursor = cursor;
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

	protected Sequence get(int n) {
		if (cursor == null || n < 1) return null;

		if (reader == null) {
			reader = new CursorReader(ThreadPool.instance(), cursor, n);
		}
		
		return reader.getTable(n);
	}

	protected long skipOver(long n) {
		if (cursor == null || n < 1) return 0;
		if (reader == null) return cursor.skip(n);
		
		Sequence seq = reader.getTable();
		if (seq != null) {
			return seq.length();
		} else {
			return 0;
		}
	}

	public synchronized void close() {
		super.close();
		if (cursor != null) {
			cursor.close();
			cursor = null;
			reader = null;
		}
	}
}
