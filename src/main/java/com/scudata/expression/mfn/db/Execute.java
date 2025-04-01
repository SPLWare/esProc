package com.scudata.expression.mfn.db;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.DBFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 执行数据库语句
 * db.execute(sql,param,...) db.execute(A,sql,param,...)
 * @author RunQian
 *
 */
public class Execute extends DBFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("execute" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) { // 没有参数
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("execute" + mm.getMessage("function.paramTypeError"));
			}

			return db.execute((String)obj, null, null, option);
		} else if (param.getType() != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("execute" + mm.getMessage("function.invalidParam"));
		}

		IParam sub0 = param.getSub(0);
		if (sub0 == null || !sub0.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("execute" + mm.getMessage("function.invalidParam"));
		}

		Object srcObj = sub0.getLeafExpression().calculate(ctx);
		if (srcObj instanceof String) {
			String strSql = (String)srcObj;
			int paramSize = param.getSubSize() - 1;
			Object []sqlParams = new Object[paramSize];
			byte []types = new byte[paramSize];
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
							throw new RQException("execute" + mm.getMessage("function.paramTypeError"));
						}

						types[i] = ((Number)tmp).byteValue();
					}
				}
			}

			return db.execute(strSql, sqlParams, types, option);
		} else if (srcObj instanceof Sequence || srcObj instanceof ICursor) {
			// 针对序列的每个元素执行sql语句，sql参数由序列元素算出
			IParam sub1 = param.getSub(1);
			if (sub1 == null || !sub1.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("execute" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("execute" + mm.getMessage("function.paramTypeError"));
			}

			String strSql = (String)obj;
			int paramSize = param.getSubSize() - 2;
			Expression []sqlParams = new Expression[paramSize];
			byte []types = new byte[paramSize];

			for (int i = 0; i < paramSize; ++i) {
				IParam sub = param.getSub(i + 2);
				if (sub == null)continue;

				if (sub.isLeaf()) { // 只有参数没有指定类型
					sqlParams[i] = sub.getLeafExpression();
				} else {
					IParam subi0 = sub.getSub(0);
					IParam subi1 = sub.getSub(1);
					if (subi0 != null) sqlParams[i] = subi0.getLeafExpression();
					if (subi1 != null) {
						Object tmp = subi1.getLeafExpression().calculate(ctx);
						if (!(tmp instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("execute" + mm.getMessage("function.paramTypeError"));
						}

						types[i] = ((Number)tmp).byteValue();
					}
				}
			}
			
			if (srcObj instanceof Sequence) {
				db.execute((Sequence)srcObj, strSql, sqlParams, types, option, ctx);
			} else {
				db.execute((ICursor)srcObj, strSql, sqlParams, types, option, ctx);
			}
			
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("execute" + mm.getMessage("function.paramTypeError"));
		}
	}
}
