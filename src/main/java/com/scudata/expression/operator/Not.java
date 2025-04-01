package com.scudata.expression.operator;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 非运算符：!
 * @author RunQian
 *
 */
public class Not extends Operator {
	public Not() {
		priority = PRI_NOT;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"!\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		right.checkValidity();
	}
	
	public Object calculate(Context ctx) {
		return Boolean.valueOf(Variant.isFalse(right.calculate(ctx)));
	}
	
	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		return right.canCalculateAll();
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray rightArray = right.calculateAll(ctx);
		return rightArray.not();
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray rightArray = right.calculateAll(ctx, signArray, sign);
		return rightArray.not();
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		IArray rightArray = right.calculateAll(ctx, result, true);
		
		for (int i = 1, size = result.size(); i <= size; ++i) {
			if (result.isTrue(i) && rightArray.isTrue(i)) {
				result.set(i, false);
			}
		}
		
		return result;
	}
}
