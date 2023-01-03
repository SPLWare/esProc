package com.scudata.expression.operator;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 运算符：\
 * 序列求差、数整除
 * @author RunQian
 *
 */
public class Diff extends Operator {
	public Diff() {
		priority = PRI_MUL;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"\\\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"\\\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);

		if (o1 == null) {
			if (o2 == null || o2 instanceof Sequence || o2 instanceof Number) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"\\\"" + mm.getMessage("function.paramTypeError"));
			}
		} else if (o1 instanceof Sequence) {
			if (o2 == null) return o1;
			if (!(o2 instanceof Sequence)) {
				Sequence s2 = new Sequence(1);
				s2.add(o2);
				o2 = s2;
			}

			// 序列求差
			return ((Sequence)o1).diff((Sequence)o2, false);
		} else {
			// 数整除
			return Variant.intDivide(o1, o2);
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray leftArray = left.calculateAll(ctx);
		IArray rightArray = right.calculateAll(ctx);
		return leftArray.memberIntDivide(rightArray);
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray leftArray = left.calculateAll(ctx, signArray, sign);
		IArray rightArray = right.calculateAll(ctx, signArray, sign);
		return leftArray.memberIntDivide(rightArray);
	}
}
