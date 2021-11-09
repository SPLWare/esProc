package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;

public class ImFunction extends Function {
	protected int m_paramSize = 0;	//参数个数
	protected boolean m_bCursor = false;
	protected boolean m_bFunctionParam = false;
	protected RfcManager m_rfcManager= null;
	protected String m_colNames[];
	protected String m_paramTypes[]; //除去jco句柄参数类型
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
			throw new RQException("jco_client" + mm.getMessage("function.missingParam"));
		}
		
		m_bFunctionParam = false;
		int size = param.getSubSize();

		int paramSize = 1 + (size-2)*2;
		Object cli = new Object();
		Object objs[] = new Object[paramSize];
		int idx = 0;
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jco_client" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				cli = param.getSub(i).getLeafExpression().calculate(ctx);
				if (cli instanceof RfcManager) {
					m_rfcManager = (RfcManager)cli;
				}else{
					MessageManager mm = EngineMessage.get();
					throw new RQException("jco_client" + mm.getMessage("function.paramTypeError"));
				}
			}else{
				if (param.getSub(i).isLeaf()){
					objs[idx] = param.getSub(i).getLeafExpression().calculate(ctx);
					if (objs[idx]!=null && objs[idx] instanceof String){
						//skip;
					}else{
						throw new RQException("jco param " + objs[i-1] + " type is not String");
					}
					idx++;
				}else if (param.getSub(i).getType()==IParam.Colon){
					if (!m_bCursor) m_bFunctionParam = true; //非游标情况下
					IParam sub = param.getSub(i);
					int nChildSize =sub.getSubSize();
					for(int j=0; j<nChildSize; j++){
						IParam subChild = sub.getSub(j);
						if (subChild!=null){
							 objs[idx++] = subChild.getLeafExpression().calculate(ctx);
						}
					}
				}						
			}
		}

		if (m_rfcManager == null){
			throw new RQException("rfcManager is null");
		}
		if (objs.length<1){
			throw new RQException("param is empty");
		}

		return doQuery(objs);
	}
	
	protected Object doQuery( Object[] objs){
		return null;
	}
	
	protected JCoFunction doJCoFunction( Object[] objs){
		JCoFunction function = null;
		do{
			function = m_rfcManager.getFunction(objs[0].toString());
			if (function==null){
				Logger.error("getFunction" + objs[0] + " false");
				break;
			}
			// check params is key:val;
			if ((objs.length-1)%2!=0){
				Logger.error("excute params is not key:value");
				break;
			}
			
			if ( m_bFunctionParam){
		        for(int i=1; i<objs.length; i+=2){
		        	String key = objs[i].toString();
		        	String val = objs[i+1].toString();
		        	JCoParameterList list = function.getImportParameterList();
		        	if (list!=null){
		        		list.setValue(key, val);
		        	}else{
		        		Logger.error("excute getImportParameterList is null");
						break;
		        	}
		        }
			}
			m_rfcManager.execute(function);	        
		}while(false);
		
		return function;
	}
	
	protected JCoParameterList doJcoOutputParam( Object[] objs){
		JCoParameterList outputParam = null;
		JCoFunction function = doJCoFunction(objs);
		if (function==null){
			System.out.println("getFunction" + objs[0] + " false");
		}else{
	        outputParam = function.getTableParameterList();
		}
		return outputParam;
	}
	
	protected JCoTable doJcoTable(Object[] objs) {
		JCoTable tb = null;
		JCoParameterList outputParam = doJcoOutputParam(objs);
		if (outputParam == null) {
			Logger.error("getTableParameterList" + objs[0] + " false");
		}else{
			tb = outputParam.getTable(objs[1].toString());			
		}

		return tb;
	}

}