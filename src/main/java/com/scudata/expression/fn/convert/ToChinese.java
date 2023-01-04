package com.scudata.expression.fn.convert;

import com.scudata.array.IArray;
import com.scudata.array.NumberArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 将数字转为中文数字写法。
 * @author runqian
 *
 */
public class ToChinese extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chn" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chn" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			boolean abbreviate = false, uppercase = false, rmb = false;
			if (option != null) {
				if (option.indexOf('a') != -1) abbreviate = true;
				if (option.indexOf('u') != -1) uppercase = true;
				if (option.indexOf('b') != -1) rmb = true;
			}
	
			if (rmb) {
				double d = ((Number)result).doubleValue();
				return StringUtils.toRMB(d, abbreviate, uppercase);
			} else {
				long l = ((Number)result).longValue();
				return StringUtils.toChinese(l, abbreviate, uppercase);
			}
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chn" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int len = array.size();
		String []resultValues = new String[len + 1];
		
		boolean abbreviate = false, uppercase = false, rmb = false;
		if (option != null) {
			if (option.indexOf('a') != -1) abbreviate = true;
			if (option.indexOf('u') != -1) uppercase = true;
			if (option.indexOf('b') != -1) rmb = true;
		}

		if (array instanceof NumberArray) {
			NumberArray numberArray = (NumberArray)array;
			for (int i = 1; i <= len; ++i) {
				if (!array.isNull(i)) {
					if (rmb) {
						double d = numberArray.getDouble(i);
						resultValues[i] = StringUtils.toRMB(d, abbreviate, uppercase);
					} else {
						long l = numberArray.getLong(i);
						resultValues[i] = StringUtils.toChinese(l, abbreviate, uppercase);
					}
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Number) {
					if (rmb) {
						double d = ((Number)obj).doubleValue();
						resultValues[i] = StringUtils.toRMB(d, abbreviate, uppercase);
					} else {
						long l = ((Number)obj).longValue();
						resultValues[i] = StringUtils.toChinese(l, abbreviate, uppercase);
					}
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("chn" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		
		StringArray result = new StringArray(resultValues, len);
		result.setTemporary(true);
		return result;
	}
}
