package com.scudata.expression.fn.datetime;

import java.util.Date;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.NumberArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Constant;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * elapse(dateExp, n) 算出相差n天/ n月/ n年的新的日期数据dateExp。
 * @author runqian
 *
 */
public class Elapse extends Function {
	public Node optimize(Context ctx) {
		boolean opt = param.optimize(ctx);
		if (opt && param.getSub(0) != null) {
			return new Constant(calculate(ctx));
		} else {
			return this;
		}
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elapse" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elapse" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam sub2 = param.getSub(1);
		if (sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elapse" + mm.getMessage("function.invalidParam"));
		}
		
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elapse" + mm.getMessage("function.paramTypeError"));
		}

		IParam sub1 = param.getSub(0);
		if (sub1 != null) {
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			return elapse(result1, ((Number)result2).intValue());
		} else {
			return elapse(null, ((Number)result2).intValue());
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IParam sub2 = param.getSub(1);
		if (sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elapse" + mm.getMessage("function.invalidParam"));
		}
		
		IArray array2 = sub2.getLeafExpression().calculateAll(ctx);
		int size = array2.size();
		IArray array1 = null;
		IParam sub1 = param.getSub(0);
		
		if (sub1 != null) {
			array1 = sub1.getLeafExpression().calculateAll(ctx);
		} else {
			Object obj = new java.sql.Timestamp(System.currentTimeMillis());
			array1 = new ConstArray(obj, size);
		}
		
		if (array1 instanceof ConstArray && array2 instanceof ConstArray) {
			Object result = elapse(array1.get(1), array2.getInt(1));
			return new ConstArray(result, size);
		} else if (array1 instanceof NumberArray && array2 instanceof ConstArray) {
			NumberArray intArray = (NumberArray)array1;
			int interval = array2.getInt(1);
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				if (!intArray.isNull(i)) {
					int date = Variant.elapse(intArray.getInt(i), interval, option);
					result.pushInt(date);
				} else {
					result.pushNull();
				}
			}
			
			return result;
		} else if (array1 instanceof NumberArray) {
			NumberArray intArray = (NumberArray)array1;
			IntArray result = new IntArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				if (!intArray.isNull(i)) {
					int date = Variant.elapse(intArray.getInt(i), array2.getInt(i), option);
					result.pushInt(date);
				} else {
					result.pushNull();
				}
			}
			
			return result;
		} else {
			ObjectArray result = new ObjectArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				Object date = elapse(array1.get(i), array2.getInt(i));
				result.push(date);
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
	
	private Object elapse(Object obj, int interval) {
		if (obj == null) {
			obj = new java.sql.Timestamp(System.currentTimeMillis());
		} else if (obj instanceof String) {
			obj = Variant.parseDate((String)obj);
			if (!(obj instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
		} else if (obj instanceof Integer) {
			int result = Variant.elapse(((Integer)obj).intValue(), interval, option);
			return ObjectCache.getInteger(result);
		} else if (!(obj instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
		}

		return Variant.elapse((Date)obj, interval, option);
	}
}
