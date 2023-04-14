package com.scudata.expression;

import java.util.List;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.NumberArray;
import com.scudata.array.ObjectArray;
import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dw.MemberFilter;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 序列元素引用
 * A(2)  A([2,4])
 * @author WangXiaoJun
 *
 */
public class ElementRef extends Function {
	private Node left;

	public ElementRef() {
		priority = PRI_SUF;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("operator.missingleftOperation"));
		}
		
		left.checkValidity();
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.invalidParam"));
		}
	}

	public void setLeft(Node node) {
		left = node;
	}

	public Node getLeft() {
		return left;
	}

	protected boolean containParam(String name) {
		if (left.containParam(name)) return true;
		return super.containParam(name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		left.getUsedParams(ctx, resultList);
		super.getUsedParams(ctx, resultList);
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		left.getUsedFields(ctx, resultList);
		super.getUsedFields(ctx, resultList);
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
		left.getUsedCells(resultList);
		super.getUsedCells(resultList);
	}

	public Node optimize(Context ctx) {
		param.optimize(ctx);
		left = left.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		Object result1 = left.calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}

		Object o = param.getLeafExpression().calculate(ctx);

		if (o == null) {
			return null;
		} else if (o instanceof Number) {
			return ((Sequence)result1).get(((Number)o).intValue());
		} else if (o instanceof Sequence) {
			return ((Sequence)result1).get((Sequence)o);
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("()" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * 对序列元素赋值
	 * @param 新值
	 * @param ctx 计算上下文
	 * @return 新值
	 */
	public Object assign(Object value, Context ctx) {
		Object result1 = left.calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}

		Sequence srcSeries = (Sequence)result1;
		int len = srcSeries.length();
		Object pval = param.getLeafExpression().calculate(ctx);

		// 越界报错，不自动补
		if (pval instanceof Number) {
			int index = ((Number)pval).intValue();
			if (index > len) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
			}

			srcSeries.set(index, value);
		} else if (pval instanceof Sequence) {
			Sequence posSeries = (Sequence)pval;
			int count = posSeries.length();
			if (value instanceof Sequence) {
				Sequence tseq = (Sequence)value;
				if (count != tseq.length()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.memCountNotMatch"));
				}

				for (int i = 1; i<= count; ++i) {
					Object posObj = posSeries.get(i);
					if (!(posObj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntSeries"));
					}

					int index = ((Number)posObj).intValue();
					if (index > len) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
					}

					srcSeries.set(index, tseq.get(i));
				}
			} else {
				for (int i = 1; i<= count; ++i) {
					Object posObj = posSeries.get(i);
					if (!(posObj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("engine.needIntSeries"));
					}

					int index = ((Number)posObj).intValue();
					if (index > len) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
					}

					srcSeries.set(index, value);
				}
			}
		} else if (pval == null) {
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.paramTypeError"));
		}

		return value;
	}
	
	/**
	 * 对序列元素做+=运算
	 * @param 值
	 * @param ctx 计算上下文
	 * @return 新值
	 */
	public Object addAssign(Object value, Context ctx) {
		Object result1 = left.calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}

		Sequence srcSeries = (Sequence)result1;
		int len = srcSeries.length();
		Object pval = param.getLeafExpression().calculate(ctx);

		// 越界报错，不自动补
		if (pval instanceof Number) {
			int index = ((Number)pval).intValue();
			if (index > len) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(index + mm.getMessage("engine.indexOutofBound"));
			}

			Object result = Variant.add(srcSeries.getMem(index), value);
			srcSeries.set(index, result);
			return result;
		} else if (pval == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	public IArray getFieldArray(Context ctx, FieldRef fieldRef) {
		IArray sequenceArray = left.calculateAll(ctx);
		IArray posArray = param.getLeafExpression().calculateAll(ctx);
		int len = sequenceArray.size();
		
		if (sequenceArray instanceof ConstArray) {
			Object obj = sequenceArray.get(1);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).getFieldValueArray(posArray, fieldRef.getName());
			} else if (obj == null) {
				return new ConstArray(null, len);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
			}
		} else {
			IArray result = new ObjectArray(len);
			for (int i = 1; i <= len; ++i) {
				Object obj = sequenceArray.get(i);
				if (obj == null) {
					result.push(null);
					continue;
				} else if (!(obj instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
				}
				
				Sequence sequence = (Sequence)obj;
				if (posArray instanceof NumberArray) {
					obj = sequence.get(posArray.getInt(i));
				} else {
					obj = posArray.get(i);
					if (obj instanceof Number) {
						obj = sequence.get(((Number)obj).intValue());
					} else if (obj instanceof Sequence) {
						obj = sequence.get((Sequence)obj);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("()" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				result.push(obj);
			}
			
			return fieldRef.getFieldArray(result);
		}
	}
	
	public IArray getFieldArray(Context ctx, FieldId fieldId) {
		IArray sequenceArray = left.calculateAll(ctx);
		IArray posArray = param.getLeafExpression().calculateAll(ctx);
		int len = sequenceArray.size();
		
		if (sequenceArray instanceof ConstArray) {
			Object obj = sequenceArray.get(1);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).getFieldValueArray(posArray, fieldId.getFieldIndex());
			} else if (obj == null) {
				return new ConstArray(null, len);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
			}
		} else {
			IArray result = new ObjectArray(len);
			for (int i = 1; i <= len; ++i) {
				Object obj = sequenceArray.get(i);
				if (obj == null) {
					result.push(null);
					continue;
				} else if (!(obj instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
				}
				
				Sequence sequence = (Sequence)obj;
				if (posArray instanceof NumberArray) {
					obj = sequence.get(posArray.getInt(i));
				} else {
					obj = posArray.get(i);
					if (obj instanceof Number) {
						obj = sequence.get(((Number)obj).intValue());
					} else if (obj instanceof Sequence) {
						obj = sequence.get((Sequence)obj);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("()" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				result.push(obj);
			}
			
			return fieldId.getFieldArray(ctx, result);
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray sequenceArray = left.calculateAll(ctx);
		IArray posArray = param.getLeafExpression().calculateAll(ctx);
		int len = sequenceArray.size();
		
		if (sequenceArray instanceof ConstArray) {
			Object obj = sequenceArray.get(1);
			if (obj == null) {
				return new ConstArray(null, len);
			} else if (!(obj instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
			}
			
			Sequence sequence = (Sequence)obj;
			if (posArray instanceof NumberArray) {
				return sequence.getMemberArray((NumberArray)posArray);
			} else {
				ObjectArray result = new ObjectArray(len);
				result.setTemporary(true);
				
				for (int i = 1; i <= len; ++i) {
					obj = posArray.get(i);
					if (obj instanceof Number) {
						obj = sequence.get(((Number)obj).intValue());
					} else if (obj instanceof Sequence) {
						obj = sequence.get((Sequence)obj);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("()" + mm.getMessage("function.paramTypeError"));
					}
					
					result.push(obj);
				}
				
				return result;
			}
		} else {
			IArray result = new ObjectArray(len);
			result.setTemporary(true);
			
			for (int i = 1; i <= len; ++i) {
				Object obj = sequenceArray.get(i);
				if (obj == null) {
					result.push(null);
					continue;
				} else if (!(obj instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
				}
				
				Sequence sequence = (Sequence)obj;
				if (posArray instanceof NumberArray) {
					obj = sequence.get(posArray.getInt(i));
				} else {
					obj = posArray.get(i);
					if (obj instanceof Number) {
						obj = sequence.get(((Number)obj).intValue());
					} else if (obj instanceof Sequence) {
						obj = sequence.get((Sequence)obj);
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("()" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				result.push(obj);
			}
			
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
		return calculateAll(ctx);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		IArray posArray = param.getLeafExpression().calculateAll(ctx);
		if (!posArray.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.paramTypeError"));
		}
		
		IArray sequenceArray = left.calculateAll(ctx);
		BoolArray result = leftResult.isTrue();
		int resultSize = result.size();
		
		if (sequenceArray instanceof ConstArray) {
			Object obj = sequenceArray.get(1);
			if (obj instanceof Sequence) {
				IArray mems = ((Sequence)obj).getMems();
				for (int i = 1; i <= resultSize; ++i) {
					if (result.isTrue(i) && (posArray.isNull(i) || mems.isFalse(posArray.getInt(i)))) {
						result.set(i, false);
					}
				}
			} else if (obj == null) {
				for (int i = 1; i <= resultSize; ++i) {
					result.set(i, false);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
			}
		} else {
			for (int i = 1; i <= resultSize; ++i) {
				if (result.isTrue(i)) {
					Object obj = sequenceArray.get(i);
					if (obj == null) {
						result.set(i, false);
					} else if (obj instanceof Sequence) {
						Sequence sequence = (Sequence)obj;
						if (posArray.isNull(i) || !sequence.isTrue(posArray.getInt(i))) {
							result.set(i, false);
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
					}
				}
			}
		}
		
		return result;
	}

	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		IArray sequenceArray = left.calculateRange(ctx);
		if (!(sequenceArray instanceof ConstArray)) {
			return Relation.PARTICALMATCH;
		}
		
		Object obj = sequenceArray.get(1);
		if (obj == null) {
			return Relation.UNMATCH;
		} else if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("dot.seriesLeft"));
		}
		
		IArray array = param.getLeafExpression().calculateRange(ctx);
		if (array == null) {
			return Relation.PARTICALMATCH;
		}
		
		Sequence sequence = (Sequence)obj;
		Object maxValue = array.get(2);
		
		int end = 0;
		if (maxValue instanceof Number) {
			end = ((Number)maxValue).intValue();
		} else if (maxValue == null) {
			return Relation.UNMATCH;
		} else {
			return Relation.PARTICALMATCH;
		}
		
		Object minValue = array.get(1);
		int start = 0;
		if (minValue instanceof Number) {
			start = ((Number)minValue).intValue();
		} else if (minValue != null) {
			return Relation.PARTICALMATCH;
		}
		
		if (end < 1) {
			return Relation.UNMATCH;
		} else if (start > 0) {
			if (end - start <= MemberFilter.MAX_CHECK_NUMBER) {
				boolean hasTrue = false, hasFalse = false;
				for (int i = start; i <= end; i++) {
					if (sequence.isTrue(i)) {
						if (hasFalse) {
							return Relation.PARTICALMATCH;
						} else {
							hasTrue = true;
						}
					} else {
						if (hasTrue) {
							return Relation.PARTICALMATCH;
						} else {
							hasFalse = true;
						}
					}
				}
				
				return hasTrue ? Relation.ALLMATCH : Relation.UNMATCH;
			} else {
				return Relation.PARTICALMATCH;
			}
		} else {
			if (end - start <= MemberFilter.MAX_CHECK_NUMBER) {
				for (int i = 1; i <= end; i++) {
					if (sequence.isTrue(i)) {
						return Relation.PARTICALMATCH;
					}
				}
				
				return Relation.UNMATCH;
			} else {
				return Relation.PARTICALMATCH;
			}
		}
	}
}
