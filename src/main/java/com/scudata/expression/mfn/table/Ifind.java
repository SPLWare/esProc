package com.scudata.expression.mfn.table;

import java.util.ArrayList;
import java.util.List;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.HashArrayIndexTable;
import com.scudata.dm.HashIndexTable;
import com.scudata.dw.MemoryTable;
import com.scudata.dw.MemoryTableIndex;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Relation;
import com.scudata.expression.TableFunction;
import com.scudata.expression.ValueList;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 利用索引对内表进行过滤
 * T.ifind(k,…;I)
 * @author LW
 *
 */
public class Ifind extends TableFunction {
	private MemoryTable prevSequence;
	private MemoryTableIndex memoryTableIndex;

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifind" + mm.getMessage("function.missingParam"));
		}
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ifind" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) return null;
			ArrayList<Expression> list = new ArrayList<Expression>();
			sub.getAllLeafExpression(list);
			
			Object key;
			int size = list.size();
			if (size == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ifind" + mm.getMessage("function.invalidParam"));
			} else if (size == 1) {
				key = list.get(0).calculate(ctx);
			} else {
				Object[] keys = new Object[size];
				for (int i = 0; i < size; i++) {
					keys[i] = list.get(i).calculate(ctx);
				}
				key = keys;
			}
			
			String iname = param.getSub(1).getLeafExpression().getIdentifierName();
			return ((MemoryTable)srcTable).ifind(key, iname, option, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifind" + mm.getMessage("function.missingParam"));
		}
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof MemoryTable;
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() == 2) {
				IParam sub = param.getSub(0);
				sub.getUsedFields(ctx, resultList);
			}
		}
	}
	
	//@1时且是hash索引时做calculateAll
	private boolean useCalculateAll(MemoryTable srcSequence) {
		IParam param = this.param;
		
		if (option == null || option.indexOf("1") == -1) {
			return false;
		}
		if (param == null || param.getType() != IParam.Semicolon) {
			return false;
		}
		if (param.getSubSize() != 2) {
			return false;
		}
		if (!param.getSub(0).isLeaf()) {
			return false;
		}
		
		if (prevSequence != srcSequence) {
			prevSequence = srcSequence;
			String iname = param.getSub(1).getLeafExpression().getIdentifierName();
			memoryTableIndex = srcSequence.getIndex(iname);
		}
		
		if (memoryTableIndex.getType() == MemoryTableIndex.TYPE_HASH)
			return true;
		else
			return false;
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
			if (leftValue instanceof MemoryTable) {
				MemoryTable srcSequence = (MemoryTable)leftValue;
				if (useCalculateAll(srcSequence)) {
					boolean hasP = option != null && option.indexOf("p") != -1;
					int []index;
					Expression exp = param.getSub(0).getLeafExpression();
					if (exp.getHome() instanceof ValueList) {
						Expression[] list = ((Function) exp.getHome()).getParamExpressions(null, true);
						int size = list.length;
						IArray[] arrays = new IArray[size];
						for (int  i = 0; i < size; i++) {
							arrays[i] = list[i].calculateAll(ctx);
						}
						HashArrayIndexTable hashIndexTable = (HashArrayIndexTable) memoryTableIndex.getIndexTable();
						index = hashIndexTable.findAllFirstPos(arrays);
					} else {
						HashIndexTable hashIndexTable = (HashIndexTable) memoryTableIndex.getIndexTable();
						IArray array = exp.calculateAll(ctx);
						index = hashIndexTable.findAllFirstPos(array);
					}
					
					if (hasP) {
						return new IntArray(index, null, index.length - 1);
					} else {
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
			if (leftValue instanceof MemoryTable) {
				MemoryTable srcSequence = (MemoryTable)leftValue;
				if (useCalculateAll(srcSequence)) {
					boolean hasP = option != null && option.indexOf("p") != -1;
					BoolArray boolArray = ArrayUtil.booleanValue(signArray, sign);
					
					int []index;
					Expression exp = param.getSub(0).getLeafExpression();
					if (exp.getHome() instanceof ValueList) {
						Expression[] list = ((Function) exp.getHome()).getParamExpressions(null, true);
						int size = list.length;
						IArray[] arrays = new IArray[size];
						for (int  i = 0; i < size; i++) {
							arrays[i] = list[i].calculateAll(ctx, boolArray, true);
						}
						HashArrayIndexTable hashIndexTable = (HashArrayIndexTable) memoryTableIndex.getIndexTable();
						index = hashIndexTable.findAllFirstPos(arrays);
					} else {
						IArray array = exp.calculateAll(ctx, boolArray, true);
						HashIndexTable hashIndexTable = (HashIndexTable) memoryTableIndex.getIndexTable();
						index = hashIndexTable.findAllFirstPos(array);
					}
					
					if (hasP) {
						return new IntArray(index, null, index.length - 1);
					} else {
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
			if (leftValue instanceof MemoryTable) {
				MemoryTable srcSequence = (MemoryTable)leftValue;
				if (useCalculateAll(srcSequence)) {
					BoolArray result = leftResult.isTrue();
					int []index;
					Expression exp = param.getSub(0).getLeafExpression();
					if (exp.getHome() instanceof ValueList) {
						Expression[] list = ((Function) exp.getHome()).getParamExpressions(null, true);
						int size = list.length;
						IArray[] arrays = new IArray[size];
						for (int  i = 0; i < size; i++) {
							arrays[i] = list[i].calculateAll(ctx, result, true);
						}
						HashArrayIndexTable hashIndexTable = (HashArrayIndexTable) memoryTableIndex.getIndexTable();
						index = hashIndexTable.findAllFirstPos(arrays);
					} else {
						IArray array = exp.calculateAll(ctx, result, true);
						HashIndexTable hashIndexTable = (HashIndexTable) memoryTableIndex.getIndexTable();
						index = hashIndexTable.findAllFirstPos(array);
					}
					
					for (int i = 1, len = index.length; i < len; ++i) {
						if (index[i] < 1) {
							result.set(i, false);
						}
					}
					
					return result;
				}
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
			if (leftValue instanceof MemoryTable) {
				IArray array = param.getSub(0).getLeafExpression().calculateRange(ctx);
				if (array == null) {
					return Relation.PARTICALMATCH;
				}
							
				MemoryTable srcSequence = (MemoryTable)leftValue;
				if (useCalculateAll(srcSequence)) {
					Object minValue = array.get(1);
					Object maxValue = array.get(2);
					
					if (Variant.isEquals(minValue, maxValue)) {
						Object value = memoryTableIndex.ifind(minValue, option, ctx);
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
		}

		return Relation.PARTICALMATCH;
	}
}
