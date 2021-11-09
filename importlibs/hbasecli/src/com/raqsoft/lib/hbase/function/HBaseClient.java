package com.raqsoft.lib.hbase.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class HBaseClient extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	//获取hdfs url, zookeeper url
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_client" + mm.getMessage("function.missingParam"));
		}
		//System.out.println("open paramSize = " + param.getSubSize());
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_client" + mm.getMessage("function.invalidParam"));
		}
	
		Object hdfs = null;//new Object();
		Object url  = null;
		if (((hdfs = ImUtils.checkValidDataType(param.getSub(0), ctx, "String"))==null) || 
			((url  = ImUtils.checkValidDataType(param.getSub(1), ctx, "String"))==null) ){
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_client" + mm.getMessage("function.paramTypeError"));
		}
		
		//String hdfs="hdfs://master/user/hbase";
        //String url="192.168.0.76:2181";//集群内网IP
		return new HbaseDriverCli(ctx, (String)hdfs, (String)url);
	}
}
