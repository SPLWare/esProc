package com.raqsoft.lib.kafka.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class ImCommit extends Function {
	
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(ImConnection.m_prjName + mm.getMessage("function.missingParam"));
		}
		option = this.getOption();
		
		Object client = param.getLeafExpression().calculate(ctx);
		if ((client instanceof ImConnection)) {
			ImConnection cli = (ImConnection)client;
			if (option!=null && option.indexOf("a")>-1){
				cli.m_consumer.commitAsync();
			}else{
				cli.m_consumer.commitSync();
			}
		}else{
			MessageManager mm = EngineMessage.get();
			throw new RQException(ImConnection.m_prjName + mm.getMessage("function.paramTypeError"));
		}
		
		return null;
	}
}
