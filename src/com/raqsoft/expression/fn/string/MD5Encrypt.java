package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MD5;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * Md5(s) ·µ»Ø×Ö´®sµÄMD5Ç©Ãû
 * @author runqian
 *
 */
public class MD5Encrypt extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("md5" + mm.getMessage("function.invalidParam"));
		}
		
		Object val = param.getLeafExpression().calculate(ctx);
		if (val instanceof String) {
			MD5 md5 = new MD5();
			return md5.getMD5ofStr((String)val);
		} else if (val == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("md5" + mm.getMessage("function.paramTypeError"));
		}
	}
}
