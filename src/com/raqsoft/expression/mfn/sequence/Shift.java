package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 移除序列指定区间的元素，用后面的元素填充
 * A.shift(i,s)
 * @author RunQian
 *
 */
public class Shift extends SequenceFunction {
	public Object calculate(Context ctx) {
		int pos = 1;
		int move = 1;
		
		if (param == null) {
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("shift" + mm.getMessage("function.paramTypeError"));
			}
			
			pos = ((Number)obj).intValue();
			if (pos < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("shift" + mm.getMessage("function.invalidParam"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("shift" + mm.getMessage("function.paramTypeError"));
				}
				
				pos = ((Number)obj).intValue();
				if (pos < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("shift" + mm.getMessage("function.invalidParam"));
				}
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				Object obj = sub1.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("shift" + mm.getMessage("function.paramTypeError"));
				}
				
				move = ((Number)obj).intValue();
				if (move < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("shift" + mm.getMessage("function.invalidParam"));
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("shift" + mm.getMessage("function.invalidParam"));
		}
		
		srcSequence.shift(pos, move);
		return srcSequence;
	}
}
