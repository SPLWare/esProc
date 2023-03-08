package com.scudata.lib.olap4j.function;

import org.olap4j.OlapConnection;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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
			throw new RQException("olap_open " + mm.getMessage("function.missingParam"));
		}
		int nSize = param.getSubSize();
		if (nSize < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("olap_open " + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		
		if (sub0 == null || sub1 == null ) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("olap_open " + mm.getMessage("function.invalidParam"));
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
			throw new RQException("olap_open " + mm.getMessage("function.paramTypeError"));
		}
				
		// String server, String catalog, String user, String password, int retry
		MdxQueryUtil mdx = new MdxQueryUtil();
		if(mdx!=null){
			OlapConnection conn = mdx.getOlapConn(ctx, url.toString(), catalog.toString(), user, pwd, 1);
			if (conn==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("olap_open " + mm.getMessage("Connect server false"));
			}
		}
		
		return mdx;
	}
}
