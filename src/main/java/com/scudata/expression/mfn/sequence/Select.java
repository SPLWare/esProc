package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;

/**
 * 取序列中满足指定条件的元素生成新的序列
 * A.select(x) A.select(xk:yk,…)
 * @author RunQian
 *
 */
public class Select extends SequenceFunction {
	public Object calculate(Context ctx) {
		Object result;
		if (param == null) {
			result = srcSequence.select(null, option, ctx);
		} else if (param.isLeaf()) {
			result = srcSequence.select(param.getLeafExpression(), option, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "select", true, true);
			Expression[] fltExps = pi.getExpressions1();
			Object[] vals = pi.getValues2(ctx);
			result = srcSequence.select(fltExps, vals, option, ctx);
		}
		
		if (option != null && option.indexOf('i') != -1) {
			if (result instanceof Table) {
				Table table = (Table)result;
				if (table.getIndexTable() == null) {
					table.createIndexTable(null);
				}
			} else if (result instanceof Sequence) {
				Sequence seq = (Sequence)result;
				if (seq.length() == 0) {
					return seq;
				} else {
					Table table = seq.derive("o");
					table.createIndexTable(null);
					return table;
				}
			}
		}
		
		return result;
	}
}
