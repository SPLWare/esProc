package com.raqsoft.lib.kafka.function;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// kafka_open(endpoint, accessKeyId, accessKeySecret, instanceName)
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
		Object o,val = null;
		boolean bTopic = false;
		int nSize = param.getSubSize();
		String fileName = null;
		List<String> topic = new ArrayList<String>();
		Properties property = new Properties(); 
		int nPartition = 0;
	
		if (param.getType()==IParam.Semicolon){			
			for(int i=0; i<nSize; i++){
				IParam subs = param.getSub(i);		
				if (subs.isLeaf()){ //for fileName
					o = subs.getLeafExpression().calculate(ctx);
					if (o instanceof FileObject){
						fileName = o.toString();
					}else if(i==2){
						nPartition = Integer.parseInt(o.toString());
					}else{
						if (bTopic){
							topic.add(o.toString());
						}else{
							File f = new File(o.toString());
							if (f.exists()){
								fileName = o.toString();
							}
						}
					}
				}else{
					for(int j=0; j<subs.getSubSize(); j++){
						IParam sub = subs.getSub(j);	
						if (i==2){
							o = sub.getLeafExpression().calculate(ctx);
							nPartition = Integer.parseInt(o.toString());
						}else if(i==0){
							o = sub.getSub(0).getLeafExpression().calculate(ctx);
							val = sub.getSub(1).getLeafExpression().calculate(ctx);
							property.put(o, val);
						}else{
							if (sub.isLeaf()){
								if (bTopic){
									o = sub.getLeafExpression().calculate(ctx);						
									topic.add(o.toString());
								}
							}
						}
					}
				}
				bTopic = true;
			}
		}else{			
			throw new RQException("client function.missingParam");
		}
		//有cluster选项时，若分区数没有写或为0时，则设置默认值为0；
		if (option!=null && option.contains("c")){
			if (nPartition==0){
				nPartition = 1;
			}
		}else if(nPartition>0){ //无cluster选项时，则nPartition参数无效.
			nPartition = 0;
		}
		if(fileName!=null){
			return new ImConnection(ctx, fileName, topic, nPartition);
		}else if(property.size()>0){
			return new ImConnection(ctx, property, topic, nPartition);
		}else{
			throw new RQException("client function.missingParam");
		}
	}

}
