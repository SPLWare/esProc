package com.scudata.expression.fn.string;

import java.net.URLDecoder;
import java.net.URLEncoder;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * ½«URL´®±àÂë´¦Àí
 * @author runqian
 *
 */
public class URLEncode extends Function {
	public Object calculate(Context ctx) {
		if (param == null || param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.missingParam"));
		}

		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub0.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("urlencode" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub1.getLeafExpression().calculate(ctx);
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
