package com.scudata.lib.datastax;

import java.util.ArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class StaxClose  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("stax_close" + mm.getMessage("function.missingParam"));
		}

		try {
			String option = this.option;
			if (option == null) option = "";
	
	//		stax_query(staxClient, cql, [value1], [value2], ...)
	//		stax_cursor(staxClient, cql, [value1], [value2], ...)	//暂时不提供预解析
	//		stax_close(staxClient)
			StaxClientImpl stax = null;
		
			ArrayList<ArrayList<ArrayList<Object>>> params = StaxClientImpl.getParams(param,ctx);
			
			if (params.size()==0) {
				throw new RQException("stax_close" + mm.getMessage("function.invalidParam"));
			}
	
			Object p000 = params.get(0).get(0).get(0);
			if (p000 == null || !(p000 instanceof StaxClientImpl)) {
				throw new RQException("stax_close" + mm.getMessage("function.invalidParam"));
			} else {
				stax = (StaxClientImpl)p000;
			}
		
			stax.close();
		} catch (Exception e) {
			throw new RQException("stax_close : " + e.getMessage());
		}
		return null;
		
	}
}
