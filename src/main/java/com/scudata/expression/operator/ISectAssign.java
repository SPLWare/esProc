package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * 运算符：^=
 * 序列求交赋值
 * @author RunQian
 *
 */
public class ISectAssign extends Operator {
	public ISectAssign() {
		priority = PRI_MUL;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"^=\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"^=\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);

		if (o1 == null) {
			if (o2 != null && !(o2 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"^\"" + mm.getMessage("function.paramTypeError"));
			}
			return null;
		} else if (o1 instanceof Sequence) {
			if (o2 == null) {
				return left.assign(null, ctx);
			}
			
			if (!(o2 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"^\"" + mm.getMessage("function.paramTypeError"));
			}

			Sequence result = ((Sequence)o1).isect((Sequence)o2, false);
			return left.assign(result, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"^\"" + mm.getMessage("function.paramTypeError"));
		}
	}

	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		return false;
	}
}
