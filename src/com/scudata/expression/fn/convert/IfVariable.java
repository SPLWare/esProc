package com.scudata.expression.fn.convert;

import java.util.List;

import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;

/**
 * ifv(name) 变量是否存在
 * @author RunQian
 *
 */
public class IfVariable extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifv" + mm.getMessage("function.invalidParam"));
		}

		String name = param.getLeafExpression().getIdentifierName();
		if (EnvUtil.getParam(name, ctx) == null) {
			return Boolean.FALSE;
		} else {
			return Boolean.TRUE;
		}
	}

	protected boolean containParam(String name) {
		return false;
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
	}
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}
}
