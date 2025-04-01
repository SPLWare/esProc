package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 判断序列是否与指定序列互为置换列
 * A.eq(B)
 * @author RunQian
 *
 */
public class Eq extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eq" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eq" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			boolean b = srcSequence.isPeq((Sequence)obj);
			return Boolean.valueOf(b);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eq" + mm.getMessage("function.paramTypeError"));
		}
	}
}
