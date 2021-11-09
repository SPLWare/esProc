package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.sap.conn.jco.JCoFunction;

public class ImExecute extends ImFunction {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		JCoFunction func = null;
		func = doJCoFunction(objs);
		if (func==null){
			Logger.error("getFunction" + objs[0] + " false");
			m_rfcManager.close();
			throw new RQException("ImExcute query false");
		}
		
		return func;
	}
}
