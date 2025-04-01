package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 创建格式化的字符串
 * format (s,…) 生成串，…表示s的参数，通过Java格式操作，使任意类型的数据转换成一个字符串。
 * @author runqian
 *
 */
public class Format extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("format" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("format" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam sub0 = param.getSub(0);
		if (sub0 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("format" + mm.getMessage("function.invalidParam"));
		}
		
		Object obj = sub0.getLeafExpression().calculate(ctx);
		String fmt;
		if (obj instanceof String) {
			fmt = (String)obj;
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("format" + mm.getMessage("function.paramTypeError"));
		} else {
			return null;
		}
		
		int size = param.getSubSize();
		Object []args = new Object[size - 1];
		for (int i = 1; i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				args[i - 1] = sub.getLeafExpression().calculate(ctx);
			}
		}
		
		return String.format(fmt, args);
	}
}
