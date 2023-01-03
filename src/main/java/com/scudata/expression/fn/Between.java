package com.scudata.expression.fn;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Relation;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 判断值是否在给定区间内
 * between(x,a:b)
 * @author RunQian
 *
 */
public class Between extends Function {
	private Expression exp;
	private Expression startExp;
	private Expression endExp;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("between" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("between" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null || sub1.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("between" + mm.getMessage("function.invalidParam"));
		}
		
		exp = sub0.getLeafExpression();
		IParam startParam = sub1.getSub(0);
		IParam endParam = sub1.getSub(1);
		if (startParam == null || endParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("between" + mm.getMessage("function.invalidParam"));
		}
		
		startExp = startParam.getLeafExpression();
		endExp = endParam.getLeafExpression();
	}
	
	public Object calculate(Context ctx) {
		Object val = exp.calculate(ctx);
		Object startVal = startExp.calculate(ctx);
		int cmp = Variant.compare(val, startVal, true);
		
		if (option == null || option.indexOf('b') == -1) {
			if (cmp > 0) {
				Object endVal = endExp.calculate(ctx);
				cmp = Variant.compare(val, endVal, true);
				if (cmp < 0) {
					return Boolean.TRUE;
				} else if (cmp == 0) {
					return Boolean.valueOf(option == null || option.indexOf('r') == -1);
				} else {
					return Boolean.FALSE;
				}
			} else if (cmp == 0) {
				return Boolean.valueOf(option == null || option.indexOf('l') == -1);
			} else {
				return Boolean.FALSE;
			}
		} else {
			if (cmp > 0) {
				Object endVal = endExp.calculate(ctx);
				cmp = Variant.compare(val, endVal, true);
				if (cmp < 0) {
					return ObjectCache.getInteger(0);
				} else if (cmp == 0) {
					if (option.indexOf('r') == -1) {
						return ObjectCache.getInteger(0);
					} else {
						return ObjectCache.getInteger(1);
					}
				} else {
					return ObjectCache.getInteger(1);
				}
			} else if (cmp == 0) {
				if (option.indexOf('l') == -1) {
					return ObjectCache.getInteger(0);
				} else {
					return ObjectCache.getInteger(-1);
				}
			} else {
				return ObjectCache.getInteger(-1);
			}
		}
	}
	

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		boolean isBinary = false;
		int leftRelation = Relation.GREATER_EQUAL;
		int rightRelation = Relation.LESS_EQUAL;
		
		if (option != null) {
			if (option.indexOf('b') != -1) isBinary = true;
			if (option.indexOf('l') != -1) leftRelation = Relation.GREATER;
			if (option.indexOf('r') != -1) rightRelation = Relation.LESS;
		}
		
		IArray array = exp.calculateAll(ctx);
		IArray startArray = startExp.calculateAll(ctx);
		
		// 与左值数组进行比较
		BoolArray result = array.calcRelation(startArray, leftRelation);
		IArray endArray = endExp.calculateAll(ctx, result, true);
		
		if (isBinary) {
			int size = result.size();
			int []values = new int[size + 1];
			boolean []signs = result.getDatas();
			for (int i = 1; i <= size; ++i) {
				values[i] = signs[i] ? 0 : -1;
			}
			
			array.calcRelations(endArray, rightRelation, result, true);
			for (int i = 1; i <= size; ++i) {
				if (values[i] == 0 && !signs[i]) {
					values[i] = 1;
				}
			}
			
			return new IntArray(values, null, size);
		} else {
			array.calcRelations(endArray, rightRelation, result, true);
			return result;
		}
	}

	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		boolean isBinary = false;
		int leftRelation = Relation.GREATER_EQUAL;
		int rightRelation = Relation.LESS_EQUAL;
		
		if (option != null) {
			if (option.indexOf('b') != -1) isBinary = true;
			if (option.indexOf('l') != -1) leftRelation = Relation.GREATER;
			if (option.indexOf('r') != -1) rightRelation = Relation.LESS;
		}
		
		BoolArray result = ArrayUtil.booleanValue(signArray, sign);
		IArray array = exp.calculateAll(ctx, result, true);
		IArray startArray = startExp.calculateAll(ctx, result, true);
		
		// 与左值数组进行比较
		array.calcRelations(startArray, leftRelation, result, true);
		IArray endArray = endExp.calculateAll(ctx, result, true);
		
		if (isBinary) {
			int size = result.size();
			int []values = new int[size + 1];
			boolean []signs = result.getDatas();
			for (int i = 1; i <= size; ++i) {
				values[i] = signs[i] ? 0 : -1;
			}
			
			array.calcRelations(endArray, rightRelation, result, true);
			for (int i = 1; i <= size; ++i) {
				if (values[i] == 0 && !signs[i]) {
					values[i] = 1;
				}
			}
			
			return new IntArray(values, null, size);
		} else {
			array.calcRelations(endArray, rightRelation, result, true);
			return result;
		}
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		int leftRelation = Relation.GREATER_EQUAL;
		int rightRelation = Relation.LESS_EQUAL;
		
		if (option != null) {
			if (option.indexOf('l') != -1) leftRelation = Relation.GREATER;
			if (option.indexOf('r') != -1) rightRelation = Relation.LESS;
		}
		
		BoolArray result = leftResult.isTrue();
		IArray array = exp.calculateAll(ctx, result, true);
		IArray startArray = startExp.calculateAll(ctx, result, true);
		
		// 与左值数组进行比较
		array.calcRelations(startArray, leftRelation, result, true);
		IArray endArray = endExp.calculateAll(ctx, result, true);
		array.calcRelations(endArray, rightRelation, result, true);
		return result;
	}
	
	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		IArray array = exp.calculateRange(ctx);
		if (array == null) {
			return Relation.PARTICALMATCH;
		}
		
		IArray startArray = startExp.calculateRange(ctx);
		if (!(startArray instanceof ConstArray)) {
			return Relation.PARTICALMATCH;
		}
		
		IArray endArray = endExp.calculateRange(ctx);
		if (!(endArray instanceof ConstArray)) {
			return Relation.PARTICALMATCH;
		}
		
		boolean containLeft = true, containRight = true;
		if (option != null) {
			if (option.indexOf('l') != -1) containLeft = false;
			if (option.indexOf('r') != -1) containRight = false;
		}
		
		Object startVal = startArray.get(1);
		Object endVal = endArray.get(1);
		Object minVal = array.get(1);
		int cmp = Variant.compare(minVal, startVal, true);
		
		if (cmp > 0 || (cmp == 0 && containLeft)) {
			cmp = Variant.compare(minVal, endVal, true);
			if (cmp < 0 || (cmp == 0 && containRight)) {
				Object maxValue = array.get(2);
				cmp = Variant.compare(maxValue, endVal, true);
				if (cmp < 0 || (cmp == 0 && containRight)) {
					return Relation.ALLMATCH;
				} else {
					return Relation.PARTICALMATCH;
				}
			} else {
				return Relation.UNMATCH;
			}
		} else {
			Object maxValue = array.get(2);
			cmp = Variant.compare(maxValue, startVal, true);
			if (cmp > 0 || (cmp == 0 && containLeft)) {
				return Relation.PARTICALMATCH;
			} else {
				return Relation.UNMATCH;
			}
		}
	}
}
