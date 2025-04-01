package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 删除序列中指定位置（多个则是位置序列）的元素
 * A.delete(k) A.delete(p)
 * @author RunQian
 *
 */
public class Delete extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Number) {
			return srcSequence.delete(((Number)obj).intValue(), option);
		} else if (obj instanceof Sequence || obj == null) {
			return srcSequence.delete((Sequence)obj, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("delete" + mm.getMessage("function.paramTypeError"));
		}
	}
}
