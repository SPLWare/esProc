package com.raqsoft.expression.fn;

import com.raqsoft.common.CellLocation;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 用行、列号（从1开始计数）计算Excel格名返回
 * cellname(r, c)
 * @author RunQian
 *
 */
public class CellName extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.invalidParam"));
		}

		IParam rowParam = param.getSub(0);
		IParam colParam = param.getSub(1);
		if (rowParam == null || colParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.invalidParam"));
		}
		
		Object row = rowParam.getLeafExpression().calculate(ctx);
		if (!(row instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.paramTypeError"));
		}
		
		Object col = colParam.getLeafExpression().calculate(ctx);
		if (!(col instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.paramTypeError"));
		}
		
		return CellLocation.getCellId(((Number)row).intValue(), ((Number)col).intValue());
	}
}
