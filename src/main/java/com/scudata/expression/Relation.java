package com.scudata.expression;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.dm.Context;

/**
 * 关系运算基类，用于定义关系常量
 * @author WangXiaoJun
 *
 */
public abstract class Relation extends Operator {
	public static final int EQUAL = 1; // 等于
	public static final int GREATER = 2; // 大于
	public static final int GREATER_EQUAL = 3; // 大于等于
	public static final int LESS  = 4; // 小于
	public static final int LESS_EQUAL = 5; // 小于等于
	public static final int NOT_EQUAL = 6; // 不等于
	public static final int AND = 7; // 逻辑与
	public static final int OR = 8; // 逻辑或

	// 匹配结果，数值需要递增
	public static final int UNMATCH = -1; // 全不匹配
	public static final int PARTICALMATCH = 0; // 部分匹配
	public static final int ALLMATCH = 1; // 全匹配
	
	/**
	 * 互换左右值的位置，取右值和左值的比较关系
	 * @param ralation 参照Relation中定义的常量
	 * @return 定义在Relation中的常量
	 */
	public static int getInverseRelation(int ralation) {
		switch(ralation) {
		case GREATER:
			return LESS;
		case GREATER_EQUAL:
			return LESS_EQUAL;
		case LESS:
			return GREATER;
		case LESS_EQUAL:
			return GREATER_EQUAL;
		default:
			return ralation;
		}
	}
	
	/**
	 * 取左值和右值的比较关系
	 * @return 定义在Relation中的常量
	 */
	public abstract int getRelation();
	
	/**
	 * 互换左右值的位置，取右值和左值的比较关系
	 * @return 定义在Relation中的常量
	 */
	public abstract int getInverseRelation();

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray leftArray = left.calculateAll(ctx);
		IArray rightArray = right.calculateAll(ctx);
		return leftArray.calcRelation(rightArray, getRelation());
	}

	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		BoolArray result = ArrayUtil.booleanValue(signArray, sign);
		IArray leftArray = left.calculateAll(ctx, result, true);
		IArray rightArray = right.calculateAll(ctx, result, true);
		leftArray.calcRelations(rightArray, getRelation(), result, true);
		return result;
	}

	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		IArray leftArray = left.calculateAll(ctx, result, true);
		IArray rightArray = right.calculateAll(ctx, result, true);
		leftArray.calcRelations(rightArray, getRelation(), result, true);
		return result;
	}
	
	/**
	 * 计算逻辑或运算符||的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult ||左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateOr(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		IArray leftArray = left.calculateAll(ctx, result, false);
		IArray rightArray = right.calculateAll(ctx, result, false);
		leftArray.calcRelations(rightArray, getRelation(), result, false);
		return result;
	}
}
