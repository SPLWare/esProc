package com.raqsoft.expression.mfn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.MemberFunction;
import com.raqsoft.parallel.ClusterMemoryTable;
import com.raqsoft.resources.EngineMessage;

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
