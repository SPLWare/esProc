package com.raqsoft.expression.mfn.db;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.DBFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 执行数据库查询语句，返回查询结果所组成的序表
 * db.query(sql,param:type,…) db.query(A,sql,param:type,…)
 * @author RunQian
 *
 */
public class Query extends DBFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("query" + mm.getMessage("function.missingParam"));
		} else if (param.getType() == IParam.Normal) { // 没有参数
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("query" + mm.getMessage("function.paramTypeError"));
			}

			if (option == null || option.indexOf('1') == -1) {
				return db.query((String)obj, null, null, option, ctx);
			} else {
				return db.query1((String)obj, null, null, option);
			}
		} else if (param.getType() == IParam.Comma) {
			IParam sub0 = param.getSub(0);
			if (sub0 == null || !sub0.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("query" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				// 针对序列的每个元素执行sql语句，sql参数由序列元素算出
				Sequence srcSeries = (Sequence)obj;
				IParam sub1 = param.getSub(1);
				if (sub1 == null || !sub1.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("query" + mm.getMessage("function.invalidParam"));
				}

				obj = sub1.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("query" + mm.getMessage("function.paramTypeError"));
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
								throw new RQException("query" + mm.getMessage("function.paramTypeError"));
							}

							types[i] = ((Number)tmp).byteValue();
						}
					}
				}

				return db.query(srcSeries, strSql, sqlParams, types, option, ctx);
			} else if (obj instanceof String) {
				String strSql = (String)obj;
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
								throw new RQException("query" + mm.getMessage("function.paramTypeError"));
							}

							types[i] = ((Number)tmp).byteValue();
						}
					}
				}

				if (option == null || option.indexOf('1') == -1) {
					return db.query(strSql, sqlParams, types, option, ctx);
				} else {
					return db.query1(strSql, sqlParams, types, option);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("query" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("query" + mm.getMessage("function.invalidParam"));
		}
	}
}
