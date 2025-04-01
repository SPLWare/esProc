package com.scudata.expression.operator;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 运算符：>=
 * @author RunQian
 *
 */
public class NotSmaller extends Relation {
	public NotSmaller() {
		priority = PRI_NSL;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\">=\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\">=\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}
	
	public Object calculate(Context ctx) {
		if (Variant.compare(left.calculate(ctx), right.calculate(ctx), true) >= 0) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}

	/**
	 * 取左值和右值的比较关系
	 * @return 定义在Relation中的常量
	 */
	public int getRelation() {
		return GREATER_EQUAL;
	}

	/**
	 * 互换左右值的位置，取右值和左值的比较关系
	 * @return 定义在Relation中的常量
	 */
	public int getInverseRelation() {
		return LESS_EQUAL;
	}

	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		IArray leftArray = left.calculateRange(ctx);
		if (leftArray == null) {
			return PARTICALMATCH;
		}
		
		IArray rightArray = right.calculateRange(ctx);
		if (rightArray instanceof ConstArray) {
			Object value = rightArray.get(1);
			int cmp1 = Variant.compare(leftArray.get(1), value, true);
			
			if (cmp1 < 0) {
				int cmp2 = Variant.compare(leftArray.get(2), value, true);
				return cmp2 < 0 ? UNMATCH : PARTICALMATCH;
			} else {
				return ALLMATCH;
			}
		} else if (leftArray instanceof ConstArray) {
			Object value = leftArray.get(1);
			int cmp1 = Variant.compare(value, rightArray.get(1), true);
			
			if (cmp1 < 0) {
				return UNMATCH;
			} else {
				int cmp2 = Variant.compare(value, rightArray.get(2), true);
				return cmp2 < 0 ? PARTICALMATCH : ALLMATCH;
			}
		} else {
			// 无法按块判断的时候则逐条判断
			return PARTICALMATCH;
		}
	}
}
