package com.scudata.expression.mfn.file;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.FileFunction;
import com.scudata.resources.EngineMessage;

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
