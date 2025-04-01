package com.scudata.expression.fn;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.NumberArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 比较两个值的大小，左值大返回1，相等返回0，左值小返回-1
 * cmp(x,y) cmp(A,B) cmp(A) cmp(A;B)
 * @author RunQian
 *
 */
public class Compare extends Function {
	private Expression exp1;
	private Expression exp2;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cmp" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			exp1 = param.getLeafExpression();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmp" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmp" + mm.getMessage("function.invalidParam"));
			}
			
			exp1 = sub1.getLeafExpression();
			exp2 = sub2.getLeafExpression();
		}
	}
	
	private int compare(Object result1, Object result2) {
		if (result1 instanceof BaseRecord) {
			if (result2 instanceof BaseRecord) {
				return ((BaseRecord)result1).compare((BaseRecord)result2);
			}
		} else if (result1 instanceof Sequence) {
			if (param.getType() == IParam.Semicolon) {
				// cmp(A;B) A可以是子表的记录，主键数多于B
				Sequence a = (Sequence)result1;
				if (result2 instanceof Sequence) {
					Sequence b = (Sequence)result2;
					if (a.length() > b.length()) {
						return a.compareTo(b, b.length());
					} else {
						return a.compareTo(b);
					}
				} else {
					if (a.length() != 0) {
						return Variant.compare(a.getMem(1), result2, true);
					} else {
						return -1;
					}
				}
			} else if (result2 instanceof Sequence) {
				return ((Sequence)result1).compareTo((Sequence)result2);
			} else if (result2 instanceof Number && ((Number)result2).intValue() == 0) {
				return ((Sequence)result1).compare0();
			}
		}
		
		return Variant.compare(result1, result2, true);
	}
	
	public Object calculate(Context ctx) {
		if (exp2 == null) {
			Object result1 = exp1.calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmp" + mm.getMessage("function.paramTypeError"));
			}
			
			int cmp = ((Sequence)result1).compare0();
			return ObjectCache.getInteger(cmp);
		} else {
			Object result1 = exp1.calculate(ctx);
			Object result2 = exp2.calculate(ctx);
			int cmp = compare(result1, result2);
			return ObjectCache.getInteger(cmp);
		}
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (exp2 == null) {
			IArray array = exp1.calculateAll(ctx);
			int len = array.size();
			
			if (array instanceof ConstArray) {
				Object val = array.get(1);
				if (!(val instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("cmp" + mm.getMessage("function.paramTypeError"));
				}
				
				int cmp = ((Sequence)val).compare0();
				return new ConstArray(ObjectCache.getInteger(cmp), len);
			} else {
				IntArray result = new IntArray(len);
				for (int i = 1; i <= len; ++i) {
					Object val = array.get(i);
					if (!(val instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cmp" + mm.getMessage("function.paramTypeError"));
					}
					
					int cmp = ((Sequence)val).compare0();
					result.pushInt(cmp);
				}
				
				return result;
			}
		} else {
			IArray array1 = exp1.calculateAll(ctx);
			IArray array2 = exp2.calculateAll(ctx);
			if (array1 instanceof NumberArray && array2 instanceof NumberArray) {
				return ((NumberArray)array1).memberCompare((NumberArray)array2);
			} else if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
				int len = array1.size();
				int cmp = compare(array1.get(1), array2.get(1));
				return new ConstArray(ObjectCache.getInteger(cmp), len);
			} else {
				int len = array1.size();
				IntArray result = new IntArray(len);
				for (int i = 1; i <= len; ++i) {
					int cmp = compare(array1.get(i), array2.get(i));
					result.pushInt(cmp);
				}
				
				return result;
			}
		}
	}
}
