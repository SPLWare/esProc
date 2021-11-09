package com.raqsoft.expression.mfn.op;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.op.Channel;
import com.raqsoft.dm.op.Push;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.OperableFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 对游标或管道附加推送数据到管道的运算
 * op.push(chi…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachPush extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("push" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Channel)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("push" + mm.getMessage("function.paramTypeError"));
			}
			
			Push push = new Push(this, (Channel)obj);
			return operable.addOperation(push, ctx);
		} else {
			for (int i = 0, size = param.getSubSize(); i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("push" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Channel)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("push" + mm.getMessage("function.paramTypeError"));
				}
				
				Push push = new Push(this, (Channel)obj);
				operable.addOperation(push, ctx);
			}
			
			return operable;
		}
	}
}
