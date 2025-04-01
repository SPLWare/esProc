package com.scudata.expression.mfn.sequence;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.Relation;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 按主键值查找记录的序号
 * A.pfind(k)
 * @author RunQian
 *
 */
public class PFind extends SequenceFunction {
	private Sequence prevSequence;
	private IndexTable indexTable;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pfind" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		boolean isSorted = false, isInsertPos = false, isZero = false;;
		if (option != null) {
			if (option.indexOf('b') != -1)isSorted = true;
			if (option.indexOf('s') != -1) {
				isSorted = true;
				isInsertPos = true;
			}
			
			if (option.indexOf('0') != -1)isZero = true;
		}

		Object key;
		if (param.isLeaf()) {
			key = param.getLeafExpression().calculate(ctx);
		} else {
			int count = param.getSubSize();
			Sequence seq = new Sequence(count);
			key = seq;
			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pfind" + mm.getMessage("function.invalidParam"));
				}
				
				seq.add(sub.getLeafExpression().calculate(ctx));
			}
		}

		int index = srcSequence.pfindByKey(key, isSorted);
		if (index <= 0 && !isInsertPos) {
			return isZero ? ObjectCache.getInteger(0) : null;
		} else {
			return ObjectCache.getInteger(index);
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
				boolean isSorted = false, isZero = false;;
				if (option != null) {
					if (option.indexOf('b') != -1)isSorted = true;
					if (option.indexOf('0') != -1)isZero = true;
				}
				
				Sequence srcSequence = (Sequence)leftValue;
				IArray array = param.getLeafExpression().calculateAll(ctx);
				int []index;
				
				if (isSorted) {
					int len = array.size();
					index = new int[len + 1];
					
					for (int i = 1; i <= len; i++) {
						index[i] = srcSequence.pfindByKey(array.get(i), true);
					}
					
					if (isZero) {
						for (int i = 1; i <= len; i++) {
							if (index[i] < 0) {
								index[i] = 0;
							}
						}
					}
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						indexTable = srcSequence.newIndexTable();
					}
					
					index = indexTable.findAllPos(array);
				}
				
				boolean []signs = null;
				if (!isZero) {
					int len = index.length;
					signs = new boolean[len];
					for (int i = 1; i < len; ++i) {
						signs[i] = index[i] < 1;
					}
				}
				
				IntArray result = new IntArray(index, signs, array.size());
				result.setTemporary(true);
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
				boolean isSorted = false, isZero = false;;
				if (option != null) {
					if (option.indexOf('b') != -1)isSorted = true;
					if (option.indexOf('0') != -1)isZero = true;
				}

				Sequence srcSequence = (Sequence)leftValue;
				BoolArray boolArray = ArrayUtil.booleanValue(signArray, sign);
				IArray array = param.getLeafExpression().calculateAll(ctx, boolArray, true);
				int []index;
				
				if (isSorted) {
					int len = array.size();
					index = new int[len + 1];
					
					for (int i = 1; i <= len; i++) {
						if (boolArray.isTrue(i)) {
							index[i] = srcSequence.pfindByKey(array.get(i), true);
						}
					}
					
					if (isZero) {
						for (int i = 1; i <= len; i++) {
							if (index[i] < 0) {
								index[i] = 0;
							}
						}
					}
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						indexTable = srcSequence.newIndexTable();
					}
					
					index = indexTable.findAllPos(array, boolArray);
				}
				
				boolean []signs = null;
				if (!isZero) {
					int len = index.length;
					signs = new boolean[len];
					for (int i = 1; i < len; ++i) {
						signs[i] = index[i] < 1;
					}
				}
				
				IntArray result = new IntArray(index, signs, array.size());
				result.setTemporary(true);
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
				
				if (option != null && option.indexOf('b') != -1) {
					int len = array.size();
					for (int i = 1; i <= len; i++) {
						if (result.isTrue(i) && srcSequence.pfindByKey(array.get(i), true) < 1) {
							result.set(i, false);
						}
					}
				} else {
					if (prevSequence != srcSequence) {
						prevSequence = srcSequence;
						indexTable = srcSequence.newIndexTable();
					}
					
					int []index = indexTable.findAllPos(array, result);
					for (int i = 1, len = index.length; i < len; ++i) {
						if (index[i] < 1) {
							result.set(i, false);
						}
					}
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
		if (option != null && option.indexOf('k') != -1) {
			return Relation.PARTICALMATCH;
		}
		
		IArray leftArray = left.calculateAll(ctx);
		if (leftArray instanceof ConstArray) {
			Object leftValue = ((ConstArray)leftArray).getData();
			if (leftValue instanceof Sequence && param.isLeaf()) {
				IArray array = param.getLeafExpression().calculateRange(ctx);
				if (array == null) {
					return Relation.PARTICALMATCH;
				}
							
				Sequence srcSequence = (Sequence)leftValue;
				Object minValue = array.get(1);
				Object maxValue = array.get(2);
				
				if (Variant.isEquals(minValue, maxValue)) {
					boolean isSorted = option != null && option.indexOf('b') != -1;
					Object value = srcSequence.findByKey(minValue, isSorted);
					if (Variant.isTrue(value)) {
						return Relation.ALLMATCH;
					} else {
						return Relation.UNMATCH;
					}
				} else {
					return Relation.PARTICALMATCH;
				}
			}
		}

		return Relation.PARTICALMATCH;
	}
}
