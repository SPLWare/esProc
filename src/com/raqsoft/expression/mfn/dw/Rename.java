package com.raqsoft.expression.mfn.dw;

import java.io.IOException;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.TableMetaDataFunction;

/**
 * 修改组表的字段名
 * T.rename(F:F’,…)
 * @author RunQian
 *
 */
public class Rename extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "rename", true, false);
		String []srcFields = pi.getExpressionStrs1();
		String []newFields = pi.getExpressionStrs2();
		try {
			table.rename(srcFields, newFields, ctx);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		return table;
	}

}
