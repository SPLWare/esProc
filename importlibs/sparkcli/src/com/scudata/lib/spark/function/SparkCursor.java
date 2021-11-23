package com.scudata.lib.spark.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class SparkCursor extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null || param.getSubSize() < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query1" + mm.getMessage(Integer.toString(param.getSubSize())));
		}

		int size = param.getSubSize();
		if (size > 6) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query2" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
	
		if (sub0 == null || sub1 == null ) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query3" + mm.getMessage("function.invalidParam"));
		}
		
		Object obj = sub0.getLeafExpression().calculate(ctx);
		if (!(obj instanceof SparkCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query4" + mm.getMessage("function.paramTypeError"));
		}
		
		SparkCli client = (SparkCli)obj;
		obj = sub1.getLeafExpression().calculate(ctx);
		if (!(obj instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query5" + mm.getMessage("function.paramTypeError"));
		}
		
		String sql = (String)obj;

		return  client.cursorQuery(sql, ctx);
	}
}
