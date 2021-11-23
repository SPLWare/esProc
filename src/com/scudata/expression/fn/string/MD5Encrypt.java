package com.scudata.expression.fn.string;

import com.scudata.common.MD5;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

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
