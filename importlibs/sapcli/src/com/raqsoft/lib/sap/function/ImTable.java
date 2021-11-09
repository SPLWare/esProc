package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoTable;

public class ImTable extends ImFunction {
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
		boolean bOK = false;
		String sMsg = "";
		do{
			JCoParameterList outputParam = m_function.getTableParameterList();
			if (outputParam == null) {
				sMsg = "getTableParameterList" + objs[0] + " false";
				Logger.error(sMsg);
				break;
			}
			
			JCoTable bt = outputParam.getTable(objs[0].toString());	
	        if (bt==null){
				sMsg = "getTable" + objs[0] + " false";
				Logger.error(sMsg);
				break;
			}
	        
	        int colSize = 0;
	        JCoRecordMetaData meta = bt.getRecordMetaData();
	        if (meta!=null){
	        	colSize = meta.getFieldCount();
	        	m_colNames = new String[colSize];
	        	for(int i=0; i<colSize; i++){
	        		m_colNames[i] = meta.getName(i);	        	
	        	}
	        }
            DataStruct ds = new DataStruct(m_colNames);
    		tb = new Table(ds, m_colNames.length);

	        for (int i = 0; i < bt.getNumRows(); i++) {
	            bt.setRow(i);
	    		Record r = tb.newLast();
	            for(int j=0; j<colSize; j++){
	        		r.setNormalFieldValue(j, bt.getValue(j));
	        	}
	        }      
			bOK = true;
		}while(false);
		
		if (!bOK){
			m_rfcManager.close();
			throw new RQException(sMsg);
		}
		return tb;
	}
}
