package com.scudata.expression.fn;

import com.scudata.app.common.AppUtil;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 函数仅用于JDBC调用 jdbccall(spl,…)。文件名无后缀时，按splx,spl,dfx顺序查找
 */
public class JDBCCall extends Function {
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jdbccall"
					+ mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		PgmCellSet pcs;
		IParam param = this.param;
		Sequence args = null;
		if (param.isLeaf()) {
			Object strObj = param.getLeafExpression().calculate(ctx);
			if (!(strObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jdbccall"
						+ mm.getMessage("function.paramTypeError"));
			}
			// 支持无后缀时按顺序查找网格文件
			try {
				pcs = AppUtil.readCellSet((String) strObj);
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
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

			// 支持无后缀时按顺序查找网格文件
			try {
				pcs = AppUtil.readCellSet((String) strObj);
			} catch (Exception e) {
				if (e instanceof RQException)
					throw (RQException) e;
				throw new RQException(e.getMessage(), e);
			}

			int size = param.getSubSize();
			args = new Sequence();
			for (int i = 1; i < size; i++) {
				IParam sub = param.getSub(i);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					args.add(obj);
				} else {
					args.add(null);
				}
			}

		}
		ParamList list = pcs.getParamList();
		if (list != null) {
			AppUtil.setParamToCellSet(pcs, args);
		}
		Context csCtx = pcs.getContext();
		csCtx.setEnv(ctx);
		pcs.calculateResult();
		return pcs;
	}
}