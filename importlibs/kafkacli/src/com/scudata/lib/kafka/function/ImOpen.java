package com.scudata.lib.kafka.function;

import java.io.File;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// kafka_open(properties; topic,...;partitionSize)
public class ImOpen extends Function {
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("client" + mm.getMessage("function.missingParam"));
		}
		Object o = null;
		int nSize = param.getSubSize();
		String fileName = null;
				
		if (nSize <2){
			throw new RQException("ParamSize is not right");
		}		
		o = param.getSub(0).getLeafExpression().calculate(ctx);		
		
		// 1.param: property
		if (o instanceof FileObject){
			fileName = o.toString();
		}else if(o instanceof String){
			File f = new File(o.toString());
			if (f.exists()){
				fileName = o.toString();
			}
		}else{
			throw new RQException("Propterty ParamType is not right");
		}
		// 2.param: topic
		String topic = null;
		o = param.getSub(1).getLeafExpression().calculate(ctx);	
		if(o instanceof String){
			topic = o.toString();
		}else{
			throw new RQException("Toptic ParamType is not right");
		}
		
		// 3.param: nPartition
		int nPartition = 1;
		if (nSize>=3 ){
			o = param.getSub(2).getLeafExpression().calculate(ctx);
			if(o instanceof Integer){
				nPartition = (Integer)o;
			}
		}
			
		if(fileName!=null && topic!=null){
			return new ImConnection(ctx, fileName, topic, nPartition);
		}else{
			throw new RQException("client function.missingParam");
		}
	}

}
