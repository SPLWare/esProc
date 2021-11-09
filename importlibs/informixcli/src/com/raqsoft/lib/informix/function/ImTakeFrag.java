package com.raqsoft.lib.informix.function;

import java.util.Map;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.informix.helper.Fragment;
import com.raqsoft.lib.informix.helper.ImSQLParser;

public class ImTakeFrag extends ImFunction {
	boolean bCursor = false;
	boolean bMultiCursor = false;
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		String option = getOption();
		if (option!=null ){
			if( option.equals("c")){
				bCursor = true;
			}
			
			if( option.equals("m")){
				bMultiCursor = true;
			}
		}
		
		Object o = super.calculate(ctx);
		
		return o;		
	}
	
	public Object doQuery( Object[] objs){
		Map<String, Fragment> map = null;
		if(objs==null){
			map = ImSQLParser.parseFragInfo(m_ifxConn.m_connect.conn, null);
		}else{
			String tableName = "kkk";
			for(Object o:objs){
				tableName+= ",'"+o.toString()+"'";
			}
			tableName = tableName.replaceFirst("kkk,", "");
			map = ImSQLParser.parseFragInfo(m_ifxConn.m_connect.conn, tableName);
		}
		if (map!=null){
			m_ifxConn.setTakeFrag(map);
			return map;
		}else{
			return null;
		}
	}
}
