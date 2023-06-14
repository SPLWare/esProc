package com.scudata.expression.fn.convert;

import java.util.Date;

import com.scudata.array.IArray;
import com.scudata.array.LongArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 将字符串、数字或日期转换成64位长整数
 * @author runqian
 *
 */
public class ToLong extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object result = param.getLeafExpression().calculate(ctx);
			if (result instanceof Long) {
				return result;
			} else if (result instanceof Number) {
				return new Long(((Number)result).longValue());
			} else if (result instanceof String) {
				try {
					return Long.parseLong((String)result);
				} catch (NumberFormatException e) {
					return null;
				}
			} else if (result instanceof Date) {
				return new Long(((Date)result).getTime());
			} else if (result == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.invalidParam"));
			}
			
			Object str = sub0.getLeafExpression().calculate(ctx);
			if (!(str instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
			
			Object radix = sub1.getLeafExpression().calculate(ctx);
			if (!(radix instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
			
			return Variant.parseLong((String)str, ((Number)radix).intValue());
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.invalidParam"));
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx);
			if (array instanceof LongArray) {
				return array;
			}
			
			int len = array.size();
			LongArray result = new LongArray(len);
			result.setTemporary(true);
			
			if (array.isNumberArray()) {
				for (int i = 1; i <= len; ++i) {
					if (array.isNull(i)) {
						result.pushNull();
					} else {
						result.pushLong(array.getLong(i));
					}
				}
			} else {
				for (int i = 1; i <= len; ++i) {
					Object obj = array.get(i);
					if (obj instanceof Number) {
						result.pushLong(((Number)obj).longValue());
					} else if (obj instanceof String) {
						result.pushLong(Long.parseLong((String)obj));
					} else if (obj instanceof Date) {
						result.pushLong(((Date)obj).getTime());
					} else if (obj == null) {
						result.pushNull();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("float" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			return result;
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.invalidParam"));
			}
			
			IArray array = sub1.getLeafExpression().calculateAll(ctx);
			Object obj = array.get(1);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
			
			int radix = ((Number)obj).intValue();
			array = sub0.getLeafExpression().calculateAll(ctx);
			int len = array.size();
			LongArray result = new LongArray(len);
			result.setTemporary(true);
			
			for (int i = 1; i <= len; ++i) {
				Object str = array.get(i);
				if (str instanceof String) {
					result.pushLong(Variant.parseLongValue((String)str, radix));
				} else if (str == null) {
					result.pushNull();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("long" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.invalidParam"));
		}
	}
}
