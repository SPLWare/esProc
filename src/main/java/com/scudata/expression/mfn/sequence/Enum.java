package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对序列做枚举分组
 * P.enum(E,y)
 * @author RunQian
 *
 */
public class Enum extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("enum" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("enum" + mm.getMessage("function.paramTypeError"));
			}

			return srcSequence.enumerate((Sequence)obj, null, option, ctx, cs);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("enum" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			if (sub0 == null || !sub0.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("enum" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("enum" + mm.getMessage("function.paramTypeError"));
			}

			IParam sub1 = param.getSub(1);
			Expression argExp = sub1 == null ? null : sub1.getLeafExpression();

			return srcSequence.enumerate((Sequence)obj, argExp, option, ctx, cs);
		}
	}
}
