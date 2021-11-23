package com.scudata.dm.cursor;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.resources.EngineMessage;

/**
 * 按位置对游标进行关联的游标
 * @author RunQian
 *
 */
public class PJoinCursor extends ICursor {
	private ICursor []cursors; // 游标数组
	private String []names; // 字段名数组
	private DataStruct ds; // 结果集数据结构
	private boolean isEnd = false; // 是否取数结束

	/**
	 * 构建按位置关联游标
	 * @param cursors 游标数组
	 * @param names 字段名数组
	 */
	public PJoinCursor(ICursor []cursors, String []names) {
		this.cursors = cursors;
		this.names = names;
		
		if (names != null) {
			ds = new DataStruct(names);
			setDataStruct(ds);
		}
	}
	
	/**
	 * 构建按位置关联游标
	 * @param cursors 游标数组
	 */
	public PJoinCursor(ICursor []cursors) {
		this(cursors, null);
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			for (ICursor cursor : cursors) {
				cursor.resetContext(ctx);
			}
			
			super.resetContext(ctx);
		}
	}

	private Sequence get_p(int n) {
		int minLen = n;
		int fcount = cursors.length;
		Sequence []tables = new Sequence[fcount];
	
		for (int i = 0; i < fcount; ++i) {
			Sequence table = cursors[i].fetch(n);
			if (table == null || table.length() == 0) {
				return null;
			}

			tables[i] = table;

			if (table.length() < minLen) minLen = table.length();
		}

		Table table = new Table(ds, minLen);
		for (int i = 1; i <= minLen; ++i) {
			Record r = table.newLast();
			for (int f = 0; f < fcount; ++f) {
				r.setNormalFieldValue(f, tables[f].getMem(i));
			}
		}

		return table;
	}
	
	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;

		if (names != null) return get_p(n);
		
		int minLen = n;
		int tcount = cursors.length;
		Sequence []tables = new Sequence[tcount];
		int []fcounts = new int[tcount];
		int fcount = 0;
		
		for (int i = 0; i < tcount; ++i) {
			Sequence table = cursors[i].fetch(n);
			if (table == null || table.length() == 0) {
				return null;
			}

			DataStruct ds = table.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}

			tables[i] = table;
			fcounts[i] = ds.getFieldCount();
			fcount += fcounts[i];

			if (table.length() < minLen) minLen = table.length();
		}

		if (ds == null) {
			String[] fnames = new String[fcount];
			int findex = 0;
			for (int i = 0; i < tcount; ++i) {
				String[] curNames = tables[i].dataStruct().getFieldNames();
				System.arraycopy(curNames, 0, fnames, findex, fcounts[i]);
				findex += fcounts[i];
			}

			ds = new DataStruct(fnames);
		}

		Table table = new Table(ds, minLen);
		Object []values = new Object[fcount];
		for (int i = 1; i <= minLen; ++i) {
			int findex = 0;
			for (int t = 0; t < tcount; ++t) {
				Record r = (Record)tables[t].getMem(i);
				Object []curVals = r.getFieldValues();
				System.arraycopy(curVals, 0, values, findex, fcounts[t]);
				findex += fcounts[t];
			}

			table.newLast(values);
		}

		return table;
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;

		ICursor []cursors = this.cursors;
		long minLen = n;
		int tcount = cursors.length;

		for (int i = 0; i < tcount; ++i) {
			long curLen = cursors[i].skip(n);
			if (curLen < 1) {
				return 0;
			}

			if (curLen < minLen) minLen = curLen;
		}

		return minLen;
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		if (cursors != null) {
			for (int i = 0, count = cursors.length; i < count; ++i) {
				cursors[i].close();
			}

			isEnd = true;
		}
	}

	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		for (int i = 0, count = cursors.length; i < count; ++i) {
			if (!cursors[i].reset()) {
				return false;
			}
		}
		
		isEnd = false;
		return true;
	}
}
