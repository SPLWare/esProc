package com.scudata.expression.mfn.sequence;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.HashIndexSequence;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.Relation;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 判断序列是否包含指定元素
 * A.contain(x)
 * @author RunQian
 *
 */
public class Contain extends SequenceFunction {
	private Sequence prevSequence = null;
	private Sequence sortedSequence = null;
	private HashIndexSequence hashIndex;
	private boolean isSorted;
	
	public void setDotLeftObject(Object obj) {
		srcSequence = (Sequence)obj;
		
		if (option != null && option.indexOf('h') != -1 && prevSequence != srcSequence) {
			prevSequence = srcSequence;
			hashIndex = new HashIndexSequence(srcSequence);
		}
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		if (option == null || option.indexOf('h') == -1) {
			srcSequence = null;
		}
	}
	
	public boolean ifModifySequence() {
		return false;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("contain" + mm.getMessage("function.missingParam"));
		}
		
		isSorted = option != null && option.indexOf('b') != -1;
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (hashIndex != null) {
				return hashIndex.findPos(obj) > 0;
			}
			
			return srcSequence.contains(obj, option != null && option.indexOf('b') != -1);
		} else {
			int size = param.getSubSize();
			if (hashIndex != null) {
				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					Object val = null;
					if (sub != null) {
						val = sub.getLeafExpression().calculate(ctx);
					}
					
					if (hashIndex.findPos(val) < 1) {
						return false;
					}
				}

				return true;
			}
			
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				Object val = null;
				if (sub != null) {
					val = sub.getLeafExpression().calculate(ctx);
				}
				
				if (!srcSequence.contains(val, isSorted)) {
					return false;
				}
			}

			return true;
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray leftArray = left.calculateAll(ctx);
		if (leftArray instanceof ConstArray) {
			Object leftValue = ((ConstArray)leftArray).getData();
			if (leftValue instanceof Sequence && param.isLeaf()) {
				Sequence srcSequence = (Sequence)leftValue;
				IArray array = param.getLeafExpression().calculateAll(ctx);
				BoolArray result = new BoolArray(true, array.size());
				result.setTemporary(true);
				
				if (isSorted) {
					srcSequence.contains(true, array, result);
				} else if (srcSequence.length() <= 3) {
					srcSequence.contains(false, array, result);
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						hashIndex = new HashIndexSequence(srcSequence);
						sortedSequence = srcSequence.sort(null);
					}
					
					hashIndex.contains(array, result);
					//sortedSequence.contains(true, array, result);
				}
				
				return result;
			}
		}
		
		return calculateAll(leftArray, ctx);
	}

	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IArray leftArray = left.calculateAll(ctx);
		if (leftArray instanceof ConstArray) {
			Object leftValue = ((ConstArray)leftArray).getData();
			if (leftValue instanceof Sequence && param.isLeaf()) {
				Sequence srcSequence = (Sequence)leftValue;
				BoolArray result = ArrayUtil.booleanValue(signArray, sign);
				IArray array = param.getLeafExpression().calculateAll(ctx, result, true);
				
				if (isSorted) {
					srcSequence.contains(true, array, result);
				} else if (srcSequence.length() <= 3) {
					srcSequence.contains(false, array, result);
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						hashIndex = new HashIndexSequence(srcSequence);
						//sortedSequence = srcSequence.sort(null);
					}
					
					hashIndex.contains(array, result);
					//sortedSequence.contains(true, array, result);
				}
				
				return result;
			}
		}
		
		return calculateAll(leftArray, ctx, signArray, sign);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		IArray leftArray = left.calculateAll(ctx);
		if (leftArray instanceof ConstArray) {
			Object leftValue = ((ConstArray)leftArray).getData();
			if (leftValue instanceof Sequence && param.isLeaf()) {
				Sequence srcSequence = (Sequence)leftValue;
				BoolArray result = leftResult.isTrue();
				IArray array = param.getLeafExpression().calculateAll(ctx, result, true);
				
				if (isSorted) {
					srcSequence.contains(true, array, result);
				} else if (srcSequence.length() <= 3) {
					srcSequence.contains(false, array, result);
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						hashIndex = new HashIndexSequence(srcSequence);
						//sortedSequence = srcSequence.sort(null);
					}
					
					hashIndex.contains(array, result);
					//sortedSequence.contains(true, array, result);
				}
				
				return result;
			}
		}
		
		return calculateAnd(leftArray, ctx, leftResult);
	}
	
	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		IArray leftArray = left.calculateAll(ctx);
		if (leftArray instanceof ConstArray) {
			Object leftValue = ((ConstArray)leftArray).getData();
			if (leftValue instanceof Sequence && param.isLeaf()) {
				IArray array = param.getLeafExpression().calculateRange(ctx);
				if (array == null) {
					return Relation.PARTICALMATCH;
				}
				
				Sequence srcSequence = (Sequence)leftValue;
				Sequence seq;
				if (option != null && option.indexOf('b') != -1) {
					seq = srcSequence;
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						sortedSequence = srcSequence.sort(null);
						hashIndex = new HashIndexSequence(srcSequence);
					}
					
					seq = sortedSequence;
				}
				
				Object minValue = array.get(1);
				Object maxValue = array.get(2);
				Object obj = seq.pos(minValue, "bs");
				int index1 = ((Number)obj).intValue();
				
				if (Variant.isEquals(minValue, maxValue)) {
					return index1 < 1 ? Relation.UNMATCH : Relation.ALLMATCH;
				} else {
					if (index1 > 0) {
						return Relation.PARTICALMATCH;
					}
					
					obj = seq.pos(maxValue, "bs");
					int index2 = ((Number)obj).intValue();
					if (index2 != index1) {
						return Relation.PARTICALMATCH;
					} else {
						return Relation.UNMATCH;
					}
				}
			}
		}

		return Relation.PARTICALMATCH;
	}
}
