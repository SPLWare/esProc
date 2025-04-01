package com.scudata.expression.fn.convert;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * bool(expression) 将表达式expression的数据类型转换为布尔型。
 * 转换规则：当参数值为null、字符串"false"(大小敏感)、布尔值false时返回false，否则返回true。
 * @author runqian
 *
 */
public class ToBool extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bool" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bool" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Boolean) {
			return obj;
		} else if (obj instanceof String) {
			if (((String)obj).equals("false")) {
				return Boolean.FALSE;
			} else {
				return Boolean.TRUE;
			}
		} else if (obj == null)  {
			return Boolean.FALSE;
		} else {
			return Boolean.TRUE;
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		if (array instanceof StringArray) {
			int len = array.size();
			StringArray strArray = (StringArray)array;
			boolean []values = new boolean[len + 1];
			
			for (int i = 1; i <= len; ++i) {
				String str = strArray.getString(i);
				values[i] = str != null && !str.equals("false");
			}
			
			BoolArray result = new BoolArray(values, len);
			result.setTemporary(true);
			return result;
		} else if (array instanceof ObjectArray) {
			int len = array.size();
			boolean []values = new boolean[len + 1];
			
			for (int i = 1; i <= len; ++i) {
				Object obj = array.get(i);
				if (obj instanceof Boolean) {
					values[i] = (Boolean)obj;
				} else if (obj instanceof String) {
					values[i] = obj != null && !obj.equals("false");
				} else {
					values[i] = obj != null;
				}
			}
			
			BoolArray result = new BoolArray(values, len);
			result.setTemporary(true);
			return result;
		} else {
			return array.isTrue();
		}
	}
}
