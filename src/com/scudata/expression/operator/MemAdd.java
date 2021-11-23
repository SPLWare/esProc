package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 运算符：++
 * 序列成员相加
 * @author RunQian
 *
 */
public class MemAdd extends Operator {
	public MemAdd() {
		priority = PRI_ADD;
	}
	
	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("operator.missingLeftOperation"));
		}

		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		Object o1 = left.calculate(ctx);
		if (!(o1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = right.calculate(ctx);
		if (!(o2 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"++\"" + mm.getMessage("function.paramTypeError"));
		}

		return Variant.memAdd((Sequence)o1, (Sequence)o2);
	}
}
