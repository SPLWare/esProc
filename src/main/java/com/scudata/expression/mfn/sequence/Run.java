package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 针对序列计算表达式，返回序列本身
 * A.run(xi,…) P.run(xi:Fi:,…)
 * @author RunQian
 *
 */
public class Run extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence;
		} else if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("run" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			Sequence []syncSequences;
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("run" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				syncSequences = new Sequence[1];
				Object val = sub.getLeafExpression().calculate(ctx);
				if (!(val instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("run" + mm.getMessage("function.paramTypeError"));
				}
				
				syncSequences[0] = (Sequence)val;
			} else {
				int size = sub.getSubSize();
				syncSequences = new Sequence[size];
				
				for (int i = 0; i < size; ++i) {
					IParam p = sub.getSub(i);
					if (p == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("run" + mm.getMessage("function.invalidParam"));
					}
					
					Object val = p.getLeafExpression().calculate(ctx);
					if (!(val instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("run" + mm.getMessage("function.paramTypeError"));
					}
					
					syncSequences[i] = (Sequence)val;
				}
			}
			
			sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("run" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				Expression exp = sub.getLeafExpression();
				srcSequence.run(exp, option, ctx, syncSequences);
			} else {
				ParamInfo2 pi = ParamInfo2.parse(sub, "run", true, false);
				Expression []exps = pi.getExpressions1();
				Expression []assignExps = pi.getExpressions2();
				srcSequence.run(assignExps, exps, option, ctx, syncSequences);
			}
			
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			srcSequence.run(exp, option, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "run", true, false);
			Expression []exps = pi.getExpressions1();
			Expression []assignExps = pi.getExpressions2();
			srcSequence.run(assignExps, exps, option, ctx);
		}

		return srcSequence;
	}
}
