package com.scudata.expression.mfn.db;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.cursor.DBCursor;
import com.scudata.expression.DBFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 创建数据库查询游标
 * db.cursor(sql,…)
 * @author RunQian
 *
 */
public class CreateCursor extends DBFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.missingParam"));
		}

		char type = param.getType();
		String strSql;
		Object []sqlParams = null;
		byte []types = null;
		if (type == IParam.Normal) { // 没有参数
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
			}

			strSql = (String)obj;
		} else if (type == IParam.Comma) {
			IParam sub0 = param.getSub(0);
			if (sub0 == null || !sub0.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
			}

			strSql = (String)obj;
			int paramSize = param.getSubSize() - 1;
			sqlParams = new Object[paramSize];
			types = new byte[paramSize];
			for (int i = 0; i < paramSize; ++i) {
				IParam sub = param.getSub(i + 1);
				if (sub == null) continue;

				if (sub.isLeaf()) { // 只有参数没有指定类型
					sqlParams[i] = sub.getLeafExpression().calculate(ctx);
				} else {
					IParam subi0 = sub.getSub(0);
					IParam subi1 = sub.getSub(1);
					if (subi0 != null) sqlParams[i] = subi0.getLeafExpression().calculate(ctx);
					if (subi1 != null) {
						Object tmp = subi1.getLeafExpression().calculate(ctx);
						if (!(tmp instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
						}

						types[i] = ((Number)tmp).byteValue();
					}
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
		}

		return new DBCursor(strSql, sqlParams, types, db, option, ctx);
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		if (obj instanceof DBObject) {
			return option == null || option.indexOf('v') == -1;
		} else {
			return false;
		}
	}
}
