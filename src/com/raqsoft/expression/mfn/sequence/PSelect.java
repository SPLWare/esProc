package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序列中满足指定条件的元素的序号
 * A.pselect(x) A.pselect(xk:yk,…)
 * @author RunQian
 *
 */
public class PSelect extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.pselect(null, option, ctx);
		} else if (param.isLeaf()) {
			return srcSequence.pselect(param.getLeafExpression(), option, ctx);
		}

		char type = param.getType();
		if (type == IParam.Colon) { // :
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pselect" + mm.getMessage("function.invalidParam"));
			}

			IParam flt = param.getSub(0);
			IParam val = param.getSub(1);
			if (flt == null || val == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pselect" + mm.getMessage("function.invalidParam"));
			}

			Expression []exps = new Expression[] {flt.getLeafExpression()};
			Object []vals = new Object[] {val.getLeafExpression().calculate(ctx)};
			return srcSequence.pselect(exps, vals , option, ctx);
		} else if (type == IParam.Comma) { // ,
			int size = param.getSubSize();
			IParam first = param.getSub(0);
			IParam last = param.getSub(size - 1);
			if (first == null || last == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pselect" + mm.getMessage("function.invalidParam"));
			}

			if (first.isLeaf()) { // s.select(x, pos)
				if (size != 2 || !last.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pselect" +
										  mm.getMessage("function.invalidParam"));
				}

				Object val = last.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pselect" + mm.getMessage("function.paramTypeError"));
				}

				int pos = ((Number)val).intValue();
				return srcSequence.pselect(first.getLeafExpression(), pos, option, ctx);
			}

			// s.select(f:x, ...)  s.select(f:x, ..., pos)
			int fltCount = last.isLeaf() ? size - 1 : size;
			Expression []flts = new Expression[fltCount];
			Object []values = new Object[fltCount];

			for (int i = 0; i < fltCount; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || sub.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pselect" + mm.getMessage("function.invalidParam"));
				}

				IParam flt = sub.getSub(0);
				IParam val = sub.getSub(1);
				if (flt == null || val == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pselect" + mm.getMessage("function.invalidParam"));
				}

				flts[i] = flt.getLeafExpression();
				values[i] = val.getLeafExpression().calculate(ctx);
			}

			if (last.isLeaf()) {
				Object val = last.getLeafExpression().calculate(ctx);
				if (!(val instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pselect" + mm.getMessage("function.paramTypeError"));
				}

				int pos = ((Number)val).intValue();
				return srcSequence.pselect(flts, values, pos, option, ctx);
			} else {
				return srcSequence.pselect(flts, values, option, ctx);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pselect" + mm.getMessage("function.invalidParam"));
		}
	}
}
