package com.scudata.expression.mfn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.MemberFunction;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;

/**
 * 根据主键值到表中查找记录
 * k.row(T)
 * @author RunQian
 *
 */
public class TableRow extends MemberFunction {
	protected Object src;
	
	public boolean isLeftTypeMatch(Object obj) {
		return true;
	}

	public void setDotLeftObject(Object obj) {
		src = obj;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("row" + mm.getMessage("function.missingParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Table) {
			return ((Table)obj).findByKey(src, false);
		} else if (obj instanceof ClusterMemoryTable) {
			return ((ClusterMemoryTable)obj).getRow((Object)src);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("row" + mm.getMessage("function.paramTypeError"));
		}
	}
}
