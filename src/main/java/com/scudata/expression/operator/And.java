package com.scudata.expression.operator;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 运算符：&&
 * @author RunQian
 *
 */
public class And extends Relation {
	public And() {
		priority = PRI_AND;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"&&\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"&&\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}

	public Object calculate(Context ctx) {
		Object value = left.calculate(ctx);
		if (Variant.isTrue(value)) {
			value = right.calculate(ctx);
			if (value instanceof Boolean) {
				return value;
			} else {
				return Boolean.valueOf(value != null);
			}
		} else {
			return Boolean.FALSE;
		}
	}

	/**
	 * 取左值和右值的比较关系
	 * @return 定义在Relation中的常量
	 */
	public int getRelation() {
		return AND;
	}
	
	/**
	 * 互换左右值的位置，取右值和左值的比较关系
	 * @return 定义在Relation中的常量
	 */
	public int getInverseRelation() {
		return AND;
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		// 左侧表达式是false时不计算右侧表达式
		IArray leftResult = left.calculateAll(ctx);
		return right.calculateAnd(ctx, leftResult);
		/*if (right instanceof Relation) {
			return ((Relation)right).calculateAnd(ctx, leftResult);
		} else {
			IArray array = right.calculateAll(ctx, leftResult, true);
			return leftResult.calcRelation(array, AND);
		}*/
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray leftResult = left.calculateAll(ctx, signArray, sign);
		if (right instanceof Relation) {
			return ((Relation)right).calculateAnd(ctx, leftResult);
		} else {
			IArray array = right.calculateAll(ctx, leftResult, true);
			return leftResult.calcRelation(array, AND);
		}
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		leftResult = left.calculateAnd(ctx, leftResult);
		return right.calculateAnd(ctx, leftResult);

		/*if (left instanceof Relation) {
			leftResult = ((Relation)left).calculateAnd(ctx, leftResult);
		} else {
			IArray array = left.calculateAll(ctx, leftResult, true);
			leftResult = leftResult.calcRelation(array, AND);
		}
		
		if (right instanceof Relation) {
			return ((Relation)right).calculateAnd(ctx, leftResult);
		} else {
			IArray array = right.calculateAll(ctx, leftResult, true);
			return leftResult.calcRelation(array, AND);
		}*/
	}
	
	/**
	 * 计算逻辑或运算符||的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult ||左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateOr(Context ctx, IArray leftResult) {
		// x1 or (x2 and x3)
		IArray result = calculateAll(ctx, leftResult, false);
		return leftResult.calcRelation(result, OR);
	}

	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		int result = left.isValueRangeMatch(ctx);
		if (result == UNMATCH) {
			return result;
		}
		
		int rightResult = right.isValueRangeMatch(ctx);
		return result < rightResult ? result : rightResult;
	}
}
