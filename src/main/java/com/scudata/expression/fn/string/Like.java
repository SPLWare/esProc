package com.scudata.expression.fn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * like( stringExp, formatExp )
 * 判断字符串是否匹配格式串。格式串中的*匹配0个或多个字符，?匹配单个字符，
 * 可以通过转义符匹配"*",例如：\*转义为*，\\转义为\
 * @author runqian
 *
 */
public class Like extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.invalidParam"));
		}

		Object o1 = sub1.getLeafExpression().calculate(ctx);
		if (o1 == null) {
			return Boolean.FALSE;
		} else if (!(o1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = sub2.getLeafExpression().calculate(ctx);
		if (o2 == null) {
			return Boolean.FALSE;
		} else if (!(o2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("like" + mm.getMessage("function.paramTypeError"));
		}

		boolean ignoreCase = option != null && option.indexOf('c') != -1;
		if (StringUtils.matches((String) o1, (String) o2, ignoreCase)) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
}
