package com.raqsoft.expression.mfn.file;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.FileFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 将对象写出到文本文件
 * f.write(s)
 * @author RunQian
 *
 */
public class Write extends FileFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("write" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("write" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		try {
			file.write(obj, option);
		} catch (java.io.IOException e) {
			throw new RQException(e.getMessage(), e);
		}

		return null;
	}
}
