package com.raqsoft.expression.mfn.op;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.op.Groupn;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.OperableFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 对游标或管道附加按序号分组运算，各组按序号写到相应的输出对象
 * op.groupn(x;F) op.groupn(x;C) op是游标或管道，F是文件序列，C是管道序列
 * @author RunQian
 *
 */
public class AttachGroupn extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupn" + mm.getMessage("function.missingParam"));
		}
		
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupn" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || !sub0.isLeaf() || sub1 == null || !sub1.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupn" + mm.getMessage("function.invalidParam"));
		}
		
		Expression exp = sub0.getLeafExpression();
		Object val = sub1.getLeafExpression().calculate(ctx);
		Sequence out = null;
		if (val instanceof Sequence) {
			out = (Sequence)val;
		} else if (val != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupn" + mm.getMessage("function.paramTypeError"));
		}

		Groupn groupn = new Groupn(this, exp, out);
		return operable.addOperation(groupn, ctx);
	}
}
