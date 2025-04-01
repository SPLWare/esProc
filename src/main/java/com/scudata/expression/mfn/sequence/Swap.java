package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 交换序列指定位置（或位置序列）的元素形成新序列返回
 * A.swap(p1, p2)
 * @author RunQian
 *
 */
public class Swap extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("swap" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("swap" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("swap" + mm.getMessage("function.invalidParam"));
		}

		Object param1 = sub0.getLeafExpression().calculate(ctx);
		if (!(param1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("swap" + mm.getMessage("function.paramTypeError"));
		}

		Object param2 = sub1.getLeafExpression().calculate(ctx);
		if (!(param2 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("swap" + mm.getMessage("function.paramTypeError"));
		}

		return srcSequence.swap((Sequence)param1, (Sequence)param2);
	}
}
