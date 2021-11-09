package com.raqsoft.expression.mfn.cursor;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.CursorFunction;

/**
 * 重置游标到数据头部
 * cs.reset()
 * @author RunQian
 *
 */
public class Reset extends CursorFunction {
	public Object calculate(Context ctx) {
		cursor.reset();
		return cursor;
	}
}
