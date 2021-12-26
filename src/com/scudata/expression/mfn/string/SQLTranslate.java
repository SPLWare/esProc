package com.scudata.expression.mfn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.sql.SQLUtil;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;

/**
 * 将标准SQL中的函数翻译成指定数据库的格式，参照表在配置中
 * sql.sqltranslate(dbtype)
 * @author RunQian
 *
 */
public class SQLTranslate extends StringFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqltranslate" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (!(obj instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqltranslate" + mm.getMessage("function.paramTypeError"));
		}
		
		if (srcStr == null || srcStr.length() == 0) return null;
		
		//int type = DBTypes.getDBType((String)obj);
		return SQLUtil.translate(srcStr, (String)obj);
	}
}