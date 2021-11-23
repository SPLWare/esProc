package com.raqsoft.lib.olap4j.function;

import org.olap4j.OlapConnection;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// olap_open(url, catalog, user, passwd)
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
			throw new RQException("ali_client" + mm.getMessage("function.missingParam"));
		}
		int nSize = param.getSubSize();
		if (nSize < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_client" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		
		if (sub0 == null || sub1 == null ) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_client" + mm.getMessage("function.invalidParam"));
		}
		
		String user="",pwd="";
		Object username = null,password= null;
		if (nSize>2){
			IParam sub2 = param.getSub(2);
			if (sub2!=null){
				username = sub2.getLeafExpression().calculate(ctx);
				if(username!=null) user = username.toString();
			}
		}
		if (nSize>3){
			IParam sub3 = param.getSub(3);
			if (sub3!=null){
				password = sub3.getLeafExpression().calculate(ctx);
				if(password!=null) pwd = password.toString();
			}
		}	
		
		Object url = sub0.getLeafExpression().calculate(ctx);
		Object catalog = sub1.getLeafExpression().calculate(ctx);
		
		if (!(url instanceof String) ) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("olap_client" + mm.getMessage("function.paramTypeError"));
		}
				
		// String server, String catalog, String user, String password, int retry
		MdxQueryUtil mdx = new MdxQueryUtil();
		if(mdx!=null){
			OlapConnection conn = mdx.getOlapConn(ctx, url.toString(), catalog.toString(), user, pwd, 1);
			if (conn==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("olap_client" + mm.getMessage("Connect server false"));
			}
		}
		
		return mdx;
	}
}
