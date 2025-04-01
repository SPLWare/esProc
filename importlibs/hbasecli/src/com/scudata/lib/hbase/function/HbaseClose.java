package com.scudata.lib.hbase.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class HbaseClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	//释放连接
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if (!(client instanceof HbaseDriverCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((HbaseDriverCli)client).close();
		return null;
	}
}
