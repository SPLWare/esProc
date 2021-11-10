package com.raqsoft.lib.hive.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.hive.function.HiveBase;
import com.raqsoft.resources.EngineMessage;

public class HiveFunction extends Function {
	protected int m_paramSize = 0;	//参数个数
	protected HiveBase m_hiveBase = null;
	protected String m_colNames[];
	protected String m_paramTypes[]; //除去hive句柄参数类型
	protected Context m_ctx;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}
	
	protected void setParamSize(int paramSize){
		m_paramSize = paramSize;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hive_client" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		//System.out.println("baseSize = " + size);

		Object cli = new Object();
		Object objs[] = new Object[size-1];
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("hive_client" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if (!(cli instanceof HiveDriverCli)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("hive_client" + mm.getMessage("function.paramTypeError"));
				}else{
					HiveDriverCli client = (HiveDriverCli)cli;
					//System.out.println("client = " + client);
					m_hiveBase = client.hiveBase;
				}
			}else{
				objs[i-1] = param.getSub(i).getLeafExpression().calculate(ctx);
				if (objs[i-1]!=null && objs[i-1] instanceof String){
					//skip;
				}else{
					throw new RQException("hive param " + objs[i-1] + " type is not String");
				}				
			}
		}

		if (m_hiveBase == null){
			throw new RQException("hivebase is null");
		}
		if (objs.length<1){
			throw new RQException("hive_param is empty");
		}

		return doQuery(objs);
	}
	
	public Object doQuery( Object[] objs){
//		m_colNames = new String[objs.length];
//		if (objs.length == 1){
//			m_colNames[0] = objs[0].toString();
//		}else{
//			m_colNames = Utils.objectArray2StringArray(objs);		
//		}
		
		return null;
	}
	
//	protected Table toTable(Object[] row){
//		System.out.println(m_colNames.length);
//		Table table = new Table(m_colNames);
//		
//		for(Object o:row){
//			System.out.println("hiveBase toTable = " + o);
//		}
//		table.newLast(row);
//		
//		return table;
//	}
//	
//	protected Table toTable(List<Object[]> rows){
//		System.out.println(m_colNames.length);
//		Table table = new Table(m_colNames);
//		
//		for(Object[] row:rows){
//			table.newLast(row);
//		}
//		//table.newLast(row);
//		
//		return table;
//	}
}