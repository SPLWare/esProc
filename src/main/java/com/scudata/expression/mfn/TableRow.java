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
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("row" + mm.getMessage("function.missingParam"));
		}
	}

	public boolean isLeftTypeMatch(Object obj) {
		return true;
	}

	public void setDotLeftObject(Object obj) {
		src = obj;
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		src = null;
	}

	public Object calculate(Context ctx) {
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
