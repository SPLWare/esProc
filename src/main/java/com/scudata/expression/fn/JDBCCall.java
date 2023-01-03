package com.scudata.expression.fn;

import com.scudata.app.common.AppUtil;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
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
			throw new RQException("jdbccall" + mm.getMessage("function.missingParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		PgmCellSet pcs;
		IParam param = this.param;
		
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
			// FileObject fo = new FileObject((String) strObj, "s");
			// pcs = fo.readPgmCellSet();
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

			// 支持无后缀时按顺序查找网格文件
			try {
				pcs = AppUtil.readCellSet((String) strObj);
			} catch (Exception e) {
				if (e instanceof RQException)
					throw (RQException) e;
				throw new RQException(e.getMessage(), e);
			}
			// FileObject fo = new FileObject((String) strObj, "s");
			// pcs = fo.readPgmCellSet();

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