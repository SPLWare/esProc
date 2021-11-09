package com.raqsoft.expression.mfn.dw;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.expression.mfn.sequence.Top;

/**
 * 读入组表数据
 * T.import(x:C,…;w;k:n)
 * @author RunQian
 *
 */
public class Import extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		ICursor cursor = CreateCursor.createCursor(table, param, option, ctx);
		Expression filter = null;
		IParam expParam = param.getSub(1);
		if (expParam == null) {
		} else if (expParam.isLeaf()) {
			filter = expParam.getLeafExpression();
		} else if (expParam.getType() == IParam.Colon) {
		} else {
			for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
				IParam sub = expParam.getSub(p);
				if (sub == null) {					
				} else if (sub.isLeaf()) {
					filter = sub.getLeafExpression();
					if (filter.getHome() instanceof Top) {
						break;
					}
				}
			}
		}
		
		if (filter != null && filter.getHome() instanceof Top) {
			return cursor.total(new Expression[]{filter}, ctx);
		} else {
			return cursor.fetch();
		}
	}
}
