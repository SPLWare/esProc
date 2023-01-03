package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 用指定值补齐序列到指定长度
 * A.pad(x,n)
 * @author RunQian
 *
 */
public class Pad extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.invalidParam"));
		}

		Object val = sub0.getLeafExpression().calculate(ctx);
		Object obj = sub1.getLeafExpression().calculate(ctx);
		if (!(obj instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.paramTypeError"));
		}

		return srcSequence.pad(val, ((Number)obj).intValue(), option);
	}
}
