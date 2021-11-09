package com.raqsoft.expression.mfn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.sql.SQLUtil;
import com.raqsoft.expression.StringFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 拆分SQL语句的各个部分构成序列返回
 * sql.sqlparse() sql.sqlparse(part)
 * @author RunQian
 *
 */
public class SQLParse extends StringFunction {
	public Object calculate(Context ctx) {
		if (srcStr == null || srcStr.length() == 0) {
			return null;
		}
		
		if (param == null) {
			return SQLUtil.parse(srcStr, option);
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqlparse" + mm.getMessage("function.paramTypeError"));
			}
			
			return SQLUtil.replace(srcStr, (String)obj, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqlparse" + mm.getMessage("function.invalidParam"));
		}
	}
}