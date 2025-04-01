package com.scudata.expression.mfn.sequence;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Relation;
import com.scudata.expression.SequenceFunction;
import com.scudata.expression.ValueList;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 根据主键值查找记录
 * A.find(k)
 * @author RunQian
 *
 */
public class Find extends SequenceFunction {
	private Sequence prevSequence;
	private IndexTable indexTable;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("find" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
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
					throw new RQException("find" + mm.getMessage("function.invalidParam"));
				}
				
				seq.add(sub.getLeafExpression().calculate(ctx));
			}
		}

		if (option != null) {
			boolean isSorted = option.indexOf('b') != -1;
			boolean isMultiRow = option.indexOf('k') != -1;
			if (isMultiRow && key instanceof Sequence) {
				Sequence keys = (Sequence)key;
				int len = keys.length();
				Sequence result = new Sequence(len);
				for (int i = 1; i <= len; ++i) {
					result.add(srcSequence.findByKey(keys.getMem(i), isSorted));
				}
				
				return result;
			} else {
				return srcSequence.findByKey(key, isSorted);
			}
		} else {
			return srcSequence.findByKey(key, false);
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
				if (prevSequence != srcSequence) {
					prevSequence = srcSequence;
					indexTable = srcSequence.newIndexTable();
				}
				
				int []index;
				Expression exp = param.getLeafExpression();
				if (exp.getHome() instanceof ValueList) {
					Expression[] list = ((Function) exp.getHome()).getParamExpressions(null, true);
					int size = list.length;
					IArray[] arrays = new IArray[size];
					for (int  i = 0; i < size; i++) {
						arrays[i] = list[i].calculateAll(ctx);
					}
					index = indexTable.findAllPos(arrays);
				} else {
					IArray array = exp.calculateAll(ctx);
					index = indexTable.findAllPos(array);
				}
				
				int len = index.length;
				Object []rs = new Object[len];
				for (int i = 1; i < len; ++i) {
					if (index[i] > 0) {
						rs[i] = srcSequence.getMem(index[i]);
					}
				}
				ObjectArray result = new ObjectArray(rs, len - 1);
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
				Sequence srcSequence = (Sequence)leftValue;
				if (prevSequence != srcSequence) {
					prevSequence = srcSequence;
					indexTable = srcSequence.newIndexTable();
				}
				
				BoolArray boolArray = ArrayUtil.booleanValue(signArray, sign);
				int []index;
				Expression exp = param.getLeafExpression();
				if (exp.getHome() instanceof ValueList) {
					Expression[] list = ((Function) exp.getHome()).getParamExpressions(null, true);
					int size = list.length;
					IArray[] arrays = new IArray[size];
					for (int  i = 0; i < size; i++) {
						arrays[i] = list[i].calculateAll(ctx, boolArray, true);
					}
					index = indexTable.findAllPos(arrays);
				} else {
					IArray array = exp.calculateAll(ctx, boolArray, true);
					index = indexTable.findAllPos(array);
				}
				
				int len = index.length;
				Object []rs = new Object[len];
				for (int i = 1; i < len; ++i) {
					if (index[i] > 0) {
						rs[i] = srcSequence.getMem(index[i]);
					}
				}
				
				ObjectArray result = new ObjectArray(rs, len - 1);
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
				if (prevSequence != srcSequence) {
					prevSequence = srcSequence;
					indexTable = srcSequence.newIndexTable();
				}
				
				BoolArray result = leftResult.isTrue();
				int []index;
				Expression exp = param.getLeafExpression();
				if (exp.getHome() instanceof ValueList) {
					Expression[] list = ((Function) exp.getHome()).getParamExpressions(null, true);
					int size = list.length;
					IArray[] arrays = new IArray[size];
					for (int  i = 0; i < size; i++) {
						arrays[i] = list[i].calculateAll(ctx, result, true);
					}
					index = indexTable.findAllPos(arrays);
				} else {
					IArray array = exp.calculateAll(ctx, result, true);
					index = indexTable.findAllPos(array);
				}
				
				for (int i = 1, len = index.length; i < len; ++i) {
					if (index[i] < 1) {
						result.set(i, false);
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
