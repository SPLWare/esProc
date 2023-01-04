package com.scudata.expression.fn.parallel;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Machines;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.parallel.Cluster;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;

// memory(h,v)
public class Memory extends Function {
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("memory" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("memory" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
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
