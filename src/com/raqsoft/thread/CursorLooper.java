package com.raqsoft.thread;

import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;

/**
 * 用于产生遍历游标数据的Job
 * @author WangXiaoJun
 *
 */
public class CursorLooper extends Job {
	private ICursor cs;

	public CursorLooper(ICursor cs) {
		this.cs = cs;
	}
	
	public void run() {
		ICursor cs = this.cs;
		while (true) {
			Sequence src = cs.fuzzyFetch(ICursor.FETCHCOUNT);
			if (src == null || src.length() == 0) break;
		}
	}
}
