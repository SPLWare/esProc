package com.scudata.expression.mfn.dw;

import com.scudata.dm.Context;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.TableMetaDataFunction;

/**
 * 读入组表数据
 * T.import(x:C,…;w;k:n)
 * @author RunQian
 *
 */
public class Import extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		ICursor cursor = CreateCursor.createCursor(table, param, option, ctx);
		return cursor.fetch();
	}
}
