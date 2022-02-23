package com.scudata.expression.mfn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.sql.SQLUtil;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;

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
			if (obj == null) {
				obj = "";
			} else if (!(obj instanceof String)) {
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