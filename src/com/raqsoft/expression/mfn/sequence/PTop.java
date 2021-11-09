package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序列前n个最小成员的序号
 * A.ptop(n) A.ptop(n,x,…)
 * @author RunQian
 *
 */
public class PTop extends SequenceFunction {
	public Object calculate(Context ctx) {
		int count = 1;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ptop" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ptop" + mm.getMessage("function.paramTypeError"));
			}
			
			return srcSequence.ptop(count, (Expression)null, option, ctx);
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ptop" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ptop" + mm.getMessage("function.paramTypeError"));
			}
			
			Expression exp = sub1.getLeafExpression();
			return srcSequence.ptop(count, exp, option, ctx);
		} else {
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ptop" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ptop" + mm.getMessage("function.paramTypeError"));
			}
			
			int size = param.getSubSize();
			Expression []exps = new Expression[size - 1];
			for (int i = 1; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ptop" + mm.getMessage("function.invalidParam"));
				}
				
				exps[i - 1] = sub.getLeafExpression();
			}
			
			return srcSequence.top(count, exps, option, ctx, true);
		}
	}
}
