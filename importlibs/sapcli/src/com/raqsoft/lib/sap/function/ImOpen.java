package com.raqsoft.lib.sap.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// hive_client(endpoint, accessKeyId, accessKeySecret, instanceName)
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
			throw new RQException("hive_client" + mm.getMessage("function.missingParam"));
		}
		Object objs[] = null;
		String opt = this.getOption();
		int size = param.getSubSize();
		
		if (size>0){ //size:2 for jco_client@f(filename:charset)
			size = param.getSubSize();
			if (size < 6 && size != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("hive_client" + mm.getMessage("function.invalidParam"));
			}
			
			objs = new Object[size];
			for(int i=0; i<size; i++){
				if (param.getSub(i) == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("hive_client" + mm.getMessage("function.invalidParam"));
				}
				objs[i] = (String)param.getSub(i).getLeafExpression().calculate(ctx);
				if (!(objs[i] instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("hive_client" + mm.getMessage("function.paramTypeError"));
				}
			}
			// ImDriverCli(String user, String passwd, String ashost, String sysnr,String client, String lang)
		}else if (opt.compareToIgnoreCase("f")==0){			
			//for jco_client@f(FileObject) or jco_client@f(filename)  
			objs = new Object[1];
			objs[0] = param.getLeafExpression().calculate(ctx);
		}
		
		ImDriverCli cli = new ImDriverCli(ctx, objs);
		return cli.getRfcManager();
	}
}
