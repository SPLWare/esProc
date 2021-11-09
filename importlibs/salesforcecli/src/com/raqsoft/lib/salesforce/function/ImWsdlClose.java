package com.raqsoft.lib.salesforce.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// ali_close(ali_client)
public class ImWsdlClose extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		try {
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sf_close " + mm.getMessage("function.missingParam"));
			}
	
			Object o = param.getLeafExpression().calculate(ctx);
			if ((o instanceof ImWsdlOpen)) {
				ImWsdlOpen cls = (ImWsdlOpen)o;
				if (cls.m_enterpriseConn!=null) {
					cls.m_enterpriseConn.logout();
				}else {
					cls.m_parterConn.logout();
				}
			}else{
				MessageManager mm = EngineMessage.get();
				throw new RQException("sf_close " + mm.getMessage("HttpPost releaseConnection false"));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
