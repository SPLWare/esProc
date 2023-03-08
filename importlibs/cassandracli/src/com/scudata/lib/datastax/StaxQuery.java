package com.scudata.lib.datastax;

import java.util.ArrayList;
import java.util.List;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class StaxQuery  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("stax_query" + mm.getMessage("function.missingParam"));
		}

		try {
			String option = this.option;
			if (option == null) option = "";
	
	//		stax_query(staxClient, cql, [value1], [value2], ...)
	//		stax_cursor(staxClient, cql, [value1], [value2], ...)	//暂时不提供预解析
	//		stax_close(staxClient)
			StaxClientImpl stax = null;
			String cql = null;
			List<Object> values = new ArrayList<Object>();
			
			ArrayList<ArrayList<ArrayList<Object>>> params = StaxClientImpl.getParams(param,ctx);
			
			if (params.size()==0) {
				throw new RQException("stax_query" + mm.getMessage("function.invalidParam"));
			}
	
			Object p000 = params.get(0).get(0).get(0);
			if (p000 == null || !(p000 instanceof StaxClientImpl)) {
				throw new RQException("stax_query" + mm.getMessage("function.invalidParam"));
			} else {
				stax = (StaxClientImpl)p000;
			}
			cql = params.get(0).get(1).get(0).toString();
			for (int i=2; i<params.get(0).size(); i++) {
				Object oi = params.get(0).get(i).get(0);
				Logger.debug("oi = " + oi);
				if (oi instanceof String) {
					try {
						oi = java.util.UUID.fromString(oi.toString());
						Logger.debug("oi = " + oi);
					} catch (Exception e) {
					}
				}
				values.add(oi);
			}
		
			return stax.query(cql, values);
		} catch (Exception e) {
			throw new RQException("stax_query : " + e.getMessage());
		}
		
	}
}
