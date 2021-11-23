package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取序列前n个最小的成员
 * A.top(n) A.top(n,x) A.top(n,y,x) A.top(n;x,…)
 * @author WangXiaoJun
 *
 */
public class Top extends SequenceFunction {
	public Object calculate(Context ctx) {
		int count = 0;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("top" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
				return srcSequence.top(count, null, option, ctx);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.paramTypeError"));
			} else {
				return null;
			}
		} else if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.paramTypeError"));
			}
			
			if (sub1.isLeaf()) {
				Expression exp = sub1.getLeafExpression();
				return srcSequence.top(count,  exp, null, option, ctx);
			} else {
				Expression []exps = sub1.toArray("top", false);
				return srcSequence.top(count, exps, option, ctx, false);
			}
		} else {
			Expression exp = null;
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.paramTypeError"));
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				exp = sub1.getLeafExpression();
			}
			
			if (size > 2) {
				IParam sub2 = param.getSub(2);
				if (sub2 != null) {
					Expression getExp = sub2.getLeafExpression();
					return srcSequence.top(count,  exp, getExp, option, ctx);
				}
			}
	
			return srcSequence.top(count, exp, option, ctx);
		}
	}
}
