package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取指定值在序列中的排名
 * A.rank(y) A.rank(y,x)
 * @author RunQian
 *
 */
public class Rank extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("rank" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		Sequence seq = srcSequence;
		Object value;
		if (param.isLeaf()) {
			value = param.getLeafExpression().calculate(ctx);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rank" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rank" + mm.getMessage("function.invalidParam"));
			}
			
			value = sub0.getLeafExpression().calculate(ctx);
			seq = seq.calc(sub1.getLeafExpression(), "o", ctx);
		}
		
		return seq.rank(value, option);
	}
}
