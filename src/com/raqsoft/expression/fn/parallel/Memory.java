package com.raqsoft.expression.fn.parallel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Machines;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.parallel.Cluster;
import com.raqsoft.parallel.ClusterMemoryTable;
import com.raqsoft.resources.EngineMessage;

// memory(h,v)
public class Memory extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("memory" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("memory" + mm.getMessage("function.invalidParam"));
		}

		IParam hubParam = param.getSub(0);
		IParam varParam = param.getSub(1);
		if (hubParam == null || varParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("memory" + mm.getMessage("function.invalidParam"));
		}

		Object hostObj = hubParam.getLeafExpression().calculate(ctx);
		Machines mc = new Machines();
		if (!mc.set(hostObj)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("memory" + mm.getMessage("function.invalidParam"));
		}

		String []hosts = mc.getHosts();
		int[] ports = mc.getPorts();
		Cluster cluster = new Cluster(hosts, ports, ctx);
		String var = varParam.getLeafExpression().toString();
		return ClusterMemoryTable.memory(cluster, var);
	}
}
