package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算y在序列A.(x)中所属的区段序号，通过区段序号获取序列中的对应成员
 * A.segp(x,y)
 * @author RunQian
 *
 */
public class Segp extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseg" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object value = param.getLeafExpression().calculate(ctx);
			int i = srcSequence.pseg(value, option);
			if (i > 0) {
				return srcSequence.getMem(i);
			} else {
				return null;
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pseg" + mm.getMessage("function.invalidParam"));
			}
			
			Sequence seq = srcSequence.calc(sub0.getLeafExpression(), "o", ctx);
			Object value = sub1.getLeafExpression().calculate(ctx);
			int i = seq.pseg(value, option);

			if (i > 0) {
				return srcSequence.getMem(i);
			} else {
				return null;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pseg" + mm.getMessage("function.invalidParam"));
		}
	}
}
