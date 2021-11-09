package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.op.IDResult;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 对序列进行去重
 * A.id() A.id(xi,…;n)
 * @author RunQian
 *
 */
public class Id extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.id(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			Sequence series = srcSequence.calc(exp, ctx);
			return series.id(option);
		}
		
		// 没有设置n参数则取出所有
		int n = Integer.MAX_VALUE;
		IParam expParam;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.invalidParam"));
			}
			
			expParam = param.getSub(0);
			if (expParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.missingParam"));
			}
			
			IParam countParam = param.getSub(1);
			if (countParam == null || !countParam.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.invalidParam"));
			}
		
			Object count = countParam.getLeafExpression().calculate(ctx);
			if (!(count instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("id" + mm.getMessage("function.paramTypeError"));
			}
			
			n = ((Number)count).intValue();
			if (n < 1) {
				return null;
			}
		} else {
			expParam = param;
		}
		
		Expression []exps;
		if (expParam.isLeaf()) {
			exps = new Expression[] {expParam.getLeafExpression()};
		} else {
			int size = expParam.getSubSize();
			exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = expParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("id" + mm.getMessage("function.missingParam"));
				}
				
				exps[i] = sub.getLeafExpression();
			}
		}
		
		IDResult id = new IDResult(exps, n, ctx);
		id.push(srcSequence, ctx);
		return id.getResultSequence();
	}
}
