package com.scudata.expression.mfn.db;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.DBFunction;
import com.scudata.resources.EngineMessage;

/**
 * 回滚数据库到指定回滚点，无参数是回滚全部更新点
 * db.rollback(spn)
 * @author RunQian
 *
 */
public class Rollback extends DBFunction {
	public Object calculate(Context ctx) {
		String name = null;
		if (param != null) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rollback" + mm.getMessage("function.paramTypeError"));
			}
			
			name = (String)obj;
		}

		return db.rollback(name);
	}
}
