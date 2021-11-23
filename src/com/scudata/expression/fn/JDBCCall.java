package com.scudata.expression.fn;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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