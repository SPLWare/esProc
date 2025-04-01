package com.scudata.expression.mfn.file;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.query.SimpleSQL;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 调用简单SQL查询
 * f.query(sql,param,…)
 * @author RunQian
 *
 */
public class Query extends FileFunction {
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

			SimpleSQL lq = new SimpleSQL(cs, (String)obj, null, ctx);
			Object val = lq.execute();
			if (val instanceof ICursor) {
				return ((ICursor)val).fetch();
			} else {
				return val;
			}
		} else if (param.getType() == IParam.Comma) {
			IParam sub0 = param.getSub(0);
			if (sub0 == null || !sub0.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("query" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("query" + mm.getMessage("function.paramTypeError"));
			}
			
			String strSql = (String)obj;
			int paramSize = param.getSubSize() - 1;
			ArrayList<Object> list = new ArrayList<Object>(paramSize);
			for (int i = 0; i < paramSize; ++i) {
				IParam sub = param.getSub(i + 1);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("query" + mm.getMessage("function.invalidParam"));
				}

				Object p = sub.getLeafExpression().calculate(ctx);
				list.add(p);
			}

			SimpleSQL lq = new SimpleSQL(cs, strSql, list, ctx);
			Object val = lq.execute();
			if (val instanceof ICursor) {
				return ((ICursor)val).fetch();
			} else {
				return val;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("query" + mm.getMessage("function.invalidParam"));
		}
	}
}
