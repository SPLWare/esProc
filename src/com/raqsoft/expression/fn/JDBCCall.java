package com.raqsoft.expression.fn;

import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.ParamList;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * 函数仅用于JDBC调用 jdbccall(dfx,…)
 *
 */
public class JDBCCall extends Function {
	public Node optimize(Context ctx) {
		if (param != null)
			param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jdbccall"
					+ mm.getMessage("function.missingParam"));
		}

		PgmCellSet pcs;
		if (param.isLeaf()) {
			Object strObj = param.getLeafExpression().calculate(ctx);
			if (!(strObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jdbccall"
						+ mm.getMessage("function.paramTypeError"));
			}

			FileObject fo = new FileObject((String) strObj, "s");
			pcs = fo.readPgmCellSet();
			// 不再使用缓存，只有call是用缓存
			// pcs = dfxManager.removeDfx((String)strObj, ctx);
		} else {
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jdbccall"
						+ mm.getMessage("function.invalidParam"));
			}

			Object strObj = sub0.getLeafExpression().calculate(ctx);
			if (!(strObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jdbccall"
						+ mm.getMessage("function.paramTypeError"));
			}

			FileObject fo = new FileObject((String) strObj, "s");
			pcs = fo.readPgmCellSet();

			// 不再使用缓存，只有call是用缓存
			// pcs = dfxManager.removeDfx((String)strObj, ctx);

			ParamList list = pcs.getParamList();
			if (list != null) {
				int size = param.getSubSize();
				if (size - 1 > list.count())
					size = list.count() + 1;

				Context curCtx = pcs.getContext();
				int paramIndex = 0;
				for (int i = 1; i < size; ++i) {
					Param p = list.get(paramIndex);
					while (p.getKind() == Param.CONST) {
						curCtx.setParamValue(p.getName(), p.getValue());
						paramIndex++;
						p = list.get(paramIndex);
					}
					paramIndex++;
					IParam sub = param.getSub(i);
					if (sub != null) {
						Object obj = sub.getLeafExpression().calculate(ctx);
						curCtx.setParamValue(p.getName(), obj);
					} else {
						curCtx.setParamValue(p.getName(), null);
					}
				}
			}
		}
		// 增加设置ctx到网格 wunan 2018-11-14
		Context csCtx = pcs.getContext();
		csCtx.setEnv(ctx);
		pcs.calculateResult();
		return pcs;
	}
}