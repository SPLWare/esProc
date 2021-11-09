package com.raqsoft.expression.mfn.dw;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dw.IndexCursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.expression.fn.Between;
import com.raqsoft.resources.EngineMessage;

/**
 * 利用组表的索引对组表进行过滤
 * T.icursor(C,…;w,I)
 * @author RunQian
 *
 */
public class Icursor extends TableMetaDataFunction {
	public Object calculate(Context ctx) {		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icursor" + mm.getMessage("function.missingParam"));
		}
		
		boolean isMultiThread = option != null && option.indexOf('m') != -1;
		String []fields = null;
		Expression w = null;
		String I = null;
		
		int segCount = 0;
		if (isMultiThread) {
			segCount = Env.getParallelNum();
		}
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2 && param.getSubSize() != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}

			IParam sub = param.getSub(0);
			if (sub == null) {
				fields = null;
			} else {
				if (sub.isLeaf()) {
					fields = new String[]{sub.getLeafExpression().getIdentifierName()};
				} else {
					int fcount = sub.getSubSize();
					fields = new String[fcount];
					for (int f = 0; f < fcount; ++f) {
						IParam p = sub.getSub(f);
						if (p == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
						}
						
						fields[f] = p.getLeafExpression().getIdentifierName();
					}
				}
			}
			
			if (param.getSubSize() == 3) {
				sub = param.getSub(2);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
					}

					segCount = ((Number)obj).intValue();
				}
			}
			
			param = param.getSub(1);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("icursor" + mm.getMessage("function.invalidParam"));
			}

		}

		IParam sub0;
		if (param.isLeaf()) {
			sub0 = param;
			I = null;
		} else {
			sub0 = param.getSub(0);
			I = (String) param.getSub(1).getLeafExpression().getIdentifierName();
		}
		
		if (sub0 != null) {
			w = sub0.getLeafExpression();
		}
		
		if (w == null) {
			w = new Expression("true");
		}
		
		//处理between表达式
		Node home = w.getHome();
		if (home instanceof Between) {
			Between bt = (Between) home;
			IParam f = bt.getParam().getSub(0);
			IParam l = bt.getParam().getSub(1).getSub(0);
			IParam r = bt.getParam().getSub(1).getSub(1);
			String field = f.getLeafExpression().getIdentifierName();
			String left = l.getLeafExpression().getIdentifierName();
			String right = r.getLeafExpression().getIdentifierName();
			w = new Expression(field + ">=" + left + "&&" + field + "<=" + right);
		}
		
		ICursor cursor = table.icursor(fields, w, I, option, ctx);
		if (segCount != 0 && cursor instanceof IndexCursor) {
			return ((IndexCursor)cursor).toMultiCursor(segCount);
		} else {
			return cursor;
		}
	}
}
