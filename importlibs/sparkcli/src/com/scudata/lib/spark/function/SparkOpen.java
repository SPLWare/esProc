package com.scudata.lib.spark.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

/*******************************
 * 连接方式：
 * spark_open()								//local
 * spark_open(hdfsUrl)						//hdfs
 * spark_open(hdfs, thrift, instanceName)	//sql
 * **/
public class SparkOpen extends ImFunction {
	public Object doQuery(Object[] objs) {
		if (objs == null) {
			return new SparkCli(this.m_ctx);
		}else if(objs.length==1 && objs[0] instanceof String){
			return new SparkCli(this.m_ctx, objs[0].toString());
		}
		
		if (objs.length!=3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_client" + mm.getMessage("function.invalidParam"));
		}
		
		for(int i=0; i<objs.length; i++){
			if (!(objs[i] instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("spark_client" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		return new SparkCli(m_ctx, (String)objs[0], (String)objs[1],(String)objs[2]);
	}
}
