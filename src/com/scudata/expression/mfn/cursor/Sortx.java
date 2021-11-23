package com.scudata.expression.mfn.cursor;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.CursorFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 对游标进行外存排序
 * cs.sortx(x…;n)
 * @author RunQian
 *
 */
public class Sortx extends CursorFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sortx" + mm.getMessage("function.missingParam"));
		}

		IParam sortParam = null;
		int capacity = -1;
		Expression gexp = null;
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() > 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}

			sortParam = param.getSub(0);
			if (sortParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}

			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				if (option != null && option.indexOf('g') != -1) {
					gexp = sub.getLeafExpression();
				} else {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (obj instanceof Number) {
						int n = ((Number)obj).intValue();
						if (n > 0) capacity = n;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sortx" + mm.getMessage("function.paramTypeError"));
					}
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}
		} else {
			sortParam = param;
		}

		Expression []exps;
		if (sortParam.isLeaf()) { // 只有一个参数
			exps = new Expression[]{ sortParam.getLeafExpression() };
		} else if (sortParam.getType() == IParam.Comma) { // ,
			int size = sortParam.getSubSize();
			exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = sortParam.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
				}
				exps[i] = sub.getLeafExpression();
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
		}

		if (gexp == null) {
			return cursor.sortx(exps, ctx, capacity, option);
		} else {
			return cursor.sortx(exps, gexp, ctx, option);
		}
	}
}
