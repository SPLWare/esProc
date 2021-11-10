package com.raqsoft.lib.hive.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// hive_client(endpoint, accessKeyId, accessKeySecret, instanceName)
public class HiveOpen extends Function {
	static public String files[] = new String[5];
	static public int fileSize = 0;
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hive_client" + mm.getMessage("function.missingParam"));
		}
		try{
			if (this.option!=null && option.compareToIgnoreCase("f")==0){
				fileSize = 0;
				if (param.isLeaf()){
					Object objs[] = new Object[1]; 
					objs[0] = param.getLeafExpression().calculate(ctx);
					files[fileSize++] = objs[0].toString();
					System.out.println("val = " + objs[0]);
				}else{
					int size = param.getSubSize();
					if (size>5){
						files = new String[size];
					}
					Object objs[] = new Object[size]; 
					for(int i=0; i<size; i++){
						if (param.getSub(i) == null ) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("hive_client" + mm.getMessage("function.invalidParam"));
						}
						objs[i] = param.getSub(i).getLeafExpression().calculate(ctx);
						if (!(objs[i] instanceof String)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("hive_client" + mm.getMessage("function.paramTypeError"));
						}
						files[fileSize++] = objs[i].toString();
					}
				}
			}else{
				int size = param.getSubSize();
				if (size != 4) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("hive_client" + mm.getMessage("function.invalidParam"));
				}
				
				Object objs[] = new Object[size];
				for(int i=0; i<size; i++){
					if (param.getSub(i) == null ) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("hive_client" + mm.getMessage("function.invalidParam"));
					}
					objs[i] = param.getSub(i).getLeafExpression().calculate(ctx);
					if (!(objs[i] instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("hive_client" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				//System.out.println("init .....");
				return new HiveDriverCli(ctx, (String)objs[0], (String)objs[1],(String)objs[2],(String)objs[3], this.option);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
}
