package com.scudata.dm.cursor;

import com.scudata.dm.*;
import com.scudata.dm.op.Operation;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.util.CursorUtil;

/**
 * 游标joinx类，（不是归并）
 * 游标与一个可分段集文件或实表T做join运算。cs数据量不大时使用这个类。
 * @author 
 * 
 */
public class CSJoinxCursor2 extends ICursor {
	private ICursor srcCursor;//源游标
	private Object []fileTable;//维表
	private Expression [][]fields;//事实表字段
	private Expression [][]keys;//维表字段
	private Expression [][]exps;//新的表达式
	private String option;
	private String fname;
	private String[][] expNames;
	
	private Sequence cache;
	private boolean isEnd;
	private int n;//缓冲区条数
	
	public CSJoinxCursor2(ICursor cursor, Expression [][]fields, Object []fileTable, 
			Expression[][] keys, Expression[][] exps, String[][] expNames, String fname, Context ctx, int n, String option) {
		srcCursor = cursor;
		this.fileTable = fileTable;
		this.fields = fields;
		this.keys = keys;
		this.exps = exps;
		this.ctx = ctx;
		this.option = option;
		this.fname = fname;
		this.expNames = expNames;
		this.n = n;
		if (this.n < ICursor.FETCHCOUNT) {
			this.n = ICursor.FETCHCOUNT;
		}
		//如果newNames里有null，则用newExps替代
		for (int i = 0, len = expNames.length; i < len; i++) {
			String[] arr = this.expNames[i];
			for (int j = 0, len2 = arr.length; j < len2; j++) {
				if (arr[j] == null) {
					arr[j] = exps[i][j].getFieldName();
				}
			}
		}
	}

	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	protected void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			exps = Operation.dupExpressions(exps, ctx);
			super.resetContext(ctx);
		}
	}

	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		Sequence temp, result;
		Sequence cache = this.cache;
		int len = 0;
		if (cache != null) {
			len = cache.length();
			if (len > n) {
				this.cache = cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		}
		
		while (true) {
			if (option != null && option.indexOf("z")!=-1) {
				temp = new MemoryTable(srcCursor, this.n);
			} else {
				temp = srcCursor.fetch(this.n);
			}
			if (temp == null || temp.length() == 0) {
				if (cache != null && cache.length() > n) {
					this.cache = cache.split(n + 1);
					return cache;
				} else {
					isEnd = true;
					this.cache = null;
					return cache;
				}
				
			}
			result = CursorUtil.joinx(temp, fields, fileTable, keys, exps, expNames, fname, ctx, option);
			if (result != null && result.length() != 0) {
				if (cache == null) {
					cache = result;
					if (n == result.length()) {
						this.cache = null;
						return result;
					}
				} else {
					cache.addAll(result);
					len = cache.length();
					if (len > n) {
						this.cache = cache.split(n + 1);
						return cache;
					} else if (len == n) {
						this.cache = null;
						return cache;
					}
				}
			}
		}
	}

	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;
		Sequence seq = get((int) n);
		if (seq != null) {
			return seq.length();
		} else {
			return 0;
		}
	}

	public synchronized void close() {
		super.close();
		srcCursor.close();
		isEnd = true;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		super.close();
		srcCursor.reset();
		isEnd = false;
		return true;
	}
}
