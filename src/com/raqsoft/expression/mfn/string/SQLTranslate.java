package com.raqsoft.expression.mfn.string;

import com.raqsoft.common.DBTypes;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.sql.SQLUtil;
import com.raqsoft.expression.StringFunction;
import com.raqsoft.resources.EngineMessage;

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