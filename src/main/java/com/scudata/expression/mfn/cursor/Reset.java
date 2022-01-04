package com.scudata.expression.mfn.cursor;

import com.scudata.dm.Context;
import com.scudata.expression.CursorFunction;

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
