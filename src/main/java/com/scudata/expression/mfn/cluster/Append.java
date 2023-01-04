package com.scudata.expression.mfn.cluster;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.ClusterPhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 追加游标数据到集群组表中
 * T.append(cs)
 * @author RunQian
 *
 */
public class Append extends ClusterPhyTableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.invalidParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof ICursor) {
			ICursor cursor = (ICursor)obj;
			table.append(cursor);
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("append" + mm.getMessage("function.paramTypeError"));
		}
		
		return table;
	}
}
