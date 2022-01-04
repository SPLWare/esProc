package com.scudata.expression.mfn.cursor;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.CursorFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 跳过指定条数的数据，省略则跳过所有，返回实际跳过的条数
 * cs.skip(n)
 * @author RunQian
 *
 */
public class Skip extends CursorFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			long n = cursor.skip();
			if (n < Integer.MAX_VALUE) {
				return new Integer((int)n);
			} else {
				return new Long(n);
			}
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("skip" + mm.getMessage("function.paramTypeError"));
			}

			long n = ((Number)obj).longValue();
			n = cursor.skip(n);
			if (n < Integer.MAX_VALUE) {
				return new Integer((int)n);
			} else {
				return new Long(n);
			}
		} else {
			if (param.getType() != IParam.Semicolon || param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("skip" + mm.getMessage("function.invalidParam"));
			}

			Expression []exps;
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("skip" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				exps = new Expression[]{sub.getLeafExpression()};
			} else {
				int count = sub.getSubSize();
				exps = new Expression[count];
				for (int i = 0; i < count; ++i) {
					IParam p = sub.getSub(i);
					if (p == null || !p.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("skip" + mm.getMessage("function.invalidParam"));
					}

					exps[i] = p.getLeafExpression();
				}
			}

			int n = cursor.skipGroup(exps, ctx);
			return new Integer(n);
		}
	}
}

