package com.scudata.expression.mfn.dw;

import com.scudata.dm.Context;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.IPhyTable;
import com.scudata.expression.PhyTableFunction;

/**
 * 读入组表数据
 * T.import(x:C,…;w;k:n)
 * @author RunQian
 *
 */
public class Import extends PhyTableFunction {
	public Object calculate(Context ctx) {
		ICursor cursor = CreateCursor.createCursor(table, param, option, ctx);
		if (option != null && option.indexOf('x') != -1) {
			CreateCursor.setOptionX(cursor, option);
		}
		return cursor.fetch();
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		if (obj instanceof IPhyTable) {
			if (option != null && option.indexOf('v') != -1)
				return false;
			return true;
		}
		
		return false;
	}
}
