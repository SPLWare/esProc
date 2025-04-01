package com.scudata.expression.fn.string;

import com.scudata.common.MD5;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * Md5(s) 返回字串s的MD5签名
 * @author runqian
 *
 */
public class MD5Encrypt extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("md5" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("md5" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
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
