package com.scudata.lib.math;

import com.scudata.common.*;
import com.scudata.resources.EngineMessage;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * ±ß½çÏÞ¶¨£ºA.range(l,u)¡£A.(max(min(~,u),l)) /
 * @author bd
 */
public class Range extends SequenceFunction {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("range" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("range" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("range" + mm.getMessage("function.invalidParam"));
		}
		
		if (sub1.isLeaf() && sub2.isLeaf()) {
			Object l = sub1.getLeafExpression().calculate(ctx);
			Object u = sub2.getLeafExpression().calculate(ctx);
			return range(srcSequence, l, u);
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException("range" + mm.getMessage("function.paramTypeError"));
	}
	
	protected static Sequence range(Sequence seq, Object l, Object u) {
		int n = seq.length();
		Sequence result = new Sequence();
		for(int i = 1; i <= n; i++){
			Object o = seq.get(i);
			Object res = o;
			if (Variant.compare(o, u) > 0 ) {
				res = u;
			}
			if (Variant.compare(res, l) < 0 ) {
				res = l;
			}
			result.add(res);
		}
		return result;
	}
}
