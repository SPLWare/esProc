package com.scudata.expression.mfn.xo;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.expression.XOFunction;
import com.scudata.resources.EngineMessage;

/**
 * 函数xo.xlsclose()。 以@r@w方式打开的Excel对象需要关闭
 *
 */
public class XlsClose extends XOFunction {
	/**
	 * 计算
	 */
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xlsclose"
					+ mm.getMessage("function.invalidParam"));
		}
		try {
			file.xlsclose();
			return null;
		} catch (RQException e) {
			throw e;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 对节点做优化
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// 对参数做优化
			param.optimize(ctx);
		}

		return this;
	}
}
