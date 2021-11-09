package com.raqsoft.lib.sap.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoStructure;

public class ImGetParam extends ImFunction {
	private JCoFunction m_function = null;
	
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("getparam " + mm.getMessage("param is null"));
		}

		int size = param.getSubSize();
		if (size!=2){
			MessageManager mm = EngineMessage.get();
			throw new RQException("getparam " + mm.getMessage("param size is not 2"));
		}
		Object func = param.getSub(0).getLeafExpression().calculate(ctx);
		if (!(func instanceof JCoFunction)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("getparam " + mm.getMessage("function.paramTypeError"));
		}
		
		m_function = (JCoFunction)func;
		Object objs[] = new Object[1];
		objs[0] = param.getSub(1).getLeafExpression().calculate(ctx);
		
		return doQuery(objs);
	}	
	
	public Object doQuery( Object[] objs){
		Table tb = null;
		do{
			JCoParameterList outputList = m_function.getExportParameterList();
	        if (outputList==null){
				System.out.println("getTableParameterList" + objs[0] + " false");
				break;
			}
	        String key = objs[objs.length - 1].toString();
	        Object val = outputList.getValue(key);
	        
	        if (val instanceof JCoStructure){
            	JCoStructure structure = (JCoStructure)val;
            	JCoMetaData meta = structure.getMetaData();
            	
            	int colSize = meta.getFieldCount();
	        	m_colNames = new String[colSize];
	        	DataStruct ds = new DataStruct(m_colNames);
         		tb = new Table(ds, m_colNames.length);
	        	for(int i=0; i<colSize; i++){
	        		m_colNames[i] = meta.getName(i);	        	
	        	}
	        	
	        	Record r = tb.newLast();
	        	for(int i=0; i<structure.getFieldCount(); i++){
	        		r.setNormalFieldValue(i, structure.getValue(i));	 
	        	}
            }else{
            	m_colNames = new String[]{"value"};	        
                DataStruct ds = new DataStruct(m_colNames);
         		tb = new Table(ds, m_colNames.length);
         		
         		Record r = tb.newLast();         		
     	        r.setNormalFieldValue(0, val);	    
            }	           	
		}while(false);
		
		return tb;
	}
}
