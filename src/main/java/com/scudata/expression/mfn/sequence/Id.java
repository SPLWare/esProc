package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.IDResult;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
			Sequence series = srcSequence.calc(exp, "o", ctx);
			series = series.id(option);
			if (option != null && option.indexOf('0') != -1) {
				series.deleteNull(false);
			}
			return series;
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
		
		IDResult id = new IDResult(exps, n, option, ctx);
		id.push(srcSequence, ctx);
		return id.getResultSequence();
	}
}
