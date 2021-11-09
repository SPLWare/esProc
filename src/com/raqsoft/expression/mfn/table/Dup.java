package com.raqsoft.expression.mfn.table;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Machines;
import com.raqsoft.expression.TableFunction;
import com.raqsoft.parallel.Cluster;
import com.raqsoft.parallel.ClusterMemoryTable;
import com.raqsoft.resources.EngineMessage;

/**
 * 把本地内表复制成集群复写内表
 * T.dup(h)
 * @author RunQian
 *
 */
public class Dup extends TableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("dup" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		Machines mc = new Machines();
		if (!mc.set(obj)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hosts" + mm.getMessage("function.invalidParam"));
		}
		
		Cluster cluster = new Cluster(mc.getHosts(), mc.getPorts(), ctx);
		return ClusterMemoryTable.dupLocal(cluster, srcTable);
	}
}
