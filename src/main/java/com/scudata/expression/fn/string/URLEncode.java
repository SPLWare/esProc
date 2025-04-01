package com.scudata.expression.fn.string;

import java.net.URLDecoder;
import java.net.URLEncoder;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 将URL串编码处理
 * @author runqian
 *
 */
public class URLEncode extends Function {
	private Expression exp1;
	private Expression exp2;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object result1 = exp1.calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = exp2.calculate(ctx);
		if (!(result2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.paramTypeError"));
		}

		try {
			if (option == null || option.indexOf('r') == -1) {
				return URLEncoder.encode((String)result1, (String)result2);
			} else {
				return URLDecoder.decode((String)result1, (String)result2);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return result1;
		}
	}
}
