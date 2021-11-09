package com.raqsoft.lib.spark.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class SparkQuery extends Function {
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
		Object ret =  client.query(sql);
		if (option!=null && option.contains("x")){
			client.close();
		}
		return ret;
	}
}
