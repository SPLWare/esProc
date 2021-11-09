package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterMemoryTableFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 为集群内表创建索引
 * T.index(n)
 * @author RunQian
 *
 */
public class MemoryIndex extends ClusterMemoryTableFunction {
	public Object calculate(Context ctx) {
		Integer capacity = null;
		if (param == null) {
			table.createIndex(capacity, option);
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.paramTypeError"));
			}
			capacity = ((Number)obj).intValue();
			table.createIndex(capacity, option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("index" + mm.getMessage("function.invalidParam"));
		}
		
		return table;
	}
}
