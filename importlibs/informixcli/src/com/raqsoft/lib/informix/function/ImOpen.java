package com.raqsoft.lib.informix.function;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.common.DBConfig;
import com.raqsoft.common.DBSession;
import com.raqsoft.common.DBTypes;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.informix.helper.IfxConn;
import com.raqsoft.resources.EngineMessage;

// ifx_open(url; fragfile)
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
			throw new RQException("ifx_client" + mm.getMessage("function.missingParam"));
		}
		int nSize = param.getSubSize();
		if (nSize < 2 ) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifx_client" + mm.getMessage("function.invalidParam"));
		}
			
		ArrayList<Expression> list1 = new ArrayList<Expression>();
		ArrayList<Expression> list2 = new ArrayList<Expression>();
		param.getSub(0).getAllLeafExpression(list1);
		param.getSub(1).getAllLeafExpression(list2);
		nSize=list1.size() + list2.size();
		
		int nOffset = 0;
		String vals[] = new String[3];
		String s = "";
		if (list1.size()==1){
			nOffset = 1;
		}
		for(int i=0; i<list1.size(); i++){
			s = ((Expression) list1.get(i)).getIdentifierName();
			vals[i+nOffset] = s.replace("\"", "");
		}
		
		for(int i=0; i<list2.size(); i++){
			Expression e = ((Expression) list2.get(i));
			if(e.isConstExpression()){
				s = e.getIdentifierName();
			}else{
				INormalCell cell = e.getHome().getSourceCell();
				s = cell.getValue().toString();
			}
			vals[i+list1.size()+nOffset] = s.replace("\"", "");
		}
				
		DBSession session = getDBSession(vals);
		return new IfxConn(ctx, session, vals[2]);
	}
	
	private DBSession getDBSession(String vals[]){
		String regExp = "jdbc:informix-sqli:.*/(.+):(.*)";
		Pattern p = Pattern.compile(regExp);
		Matcher match = p.matcher(vals[1]);
		
		String dbName = "";
		if (match.find()) {
			 dbName = match.group(1);				 
		}
		
		String url=null, user=null, pwd = null;
		String ary[] = vals[1].toLowerCase().split("&");
		if (ary.length==3){
			url = ary[0];
			user = ary[1].replaceAll("user=", "");
			pwd = ary[2].replaceAll("pwd=", "");
		}
		
		DBConfig cfg = new DBConfig();
		String drv = vals[0];
		if (drv==null){
			drv = "com.informix.jdbc.IfxDriver";
    	}
		
		cfg.setName(dbName);
		cfg.setDriver(drv);
		cfg.setUrl(url);
		cfg.setUser(user);
		cfg.setPassword(pwd);
		cfg.setDBType(DBTypes.INFMIX);
		cfg.setAccessPrivilege(true);

		DBSession session = new DBSession("informix", cfg);
		
		return session;
	}
}
