package com.scudata.expression.mfn.table;

import com.scudata.dm.Context;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.TableFunction;

/**
 * 对序表的字段进行重命名
 * T.rename(F:F',…)
 * @author RunQian
 *
 */
public class Rename extends TableFunction {
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "rename", true, false);
		String []srcFields = pi.getExpressionStrs1();
		String []newFields = pi.getExpressionStrs2();
		srcTable.rename(srcFields, newFields);
		return srcTable;
	}
}
