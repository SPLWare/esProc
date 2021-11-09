package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoTable;

public class ImCursorImpl extends ICursor {
	private Context m_ctx;
	private boolean bEnd = false;
	private JCoTable m_jcoTable;
	
	public ImCursorImpl(JCoTable bt, Context ctx) {
		this.m_jcoTable = bt;
		this.m_ctx = ctx;
		ctx.addResource(this);		
	}

	public synchronized void close() {
		super.close();
		
		try {
			if (m_ctx != null) {
				m_ctx.removeResource(this);			
				m_ctx = null;
			}
			bEnd = true;
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}

	protected long skipOver(long n) {
		if(m_jcoTable == null){
			Logger.error("jcoTalbe is null");
			return 0;
		}
		
		int nCount = 0;
        do{
        	if (++nCount>=n){
        		m_jcoTable.nextRow();
        		break;
        	}
        }while(m_jcoTable.nextRow());
		
		return nCount;
	}
	
	public Sequence get(int n) {
		if(m_jcoTable == null){
			Logger.info("jcoTalbe is null");
			return null;
		}
		
		Table tb = null;		
        int colSize = 0;
        String colNames[] = null;
        JCoRecordMetaData meta = m_jcoTable.getRecordMetaData();
        if (meta!=null){
        	colSize = meta.getFieldCount();
        	colNames = new String[colSize];
        	for(int i=0; i<colSize; i++){
        		colNames[i] = meta.getName(i);	        	
        	}
       
	        DataStruct ds = new DataStruct(colNames);
			tb = new Table(ds, colNames.length);
        }else{
        	throw new RQException("record no metadata");
        }

        int nCount = 0;
        do{
        	Record r = tb.newLast();
        	nCount++;
        	for(int j=0; j<colSize; j++){        	  
        	  r.setNormalFieldValue(j, m_jcoTable.getValue(j));
      	    }
        	if (nCount>=n){
        		m_jcoTable.nextRow();
        		break;
        	}
        }while(m_jcoTable.nextRow());
        		
		return tb;
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	public boolean isEnd()
	{
		return bEnd;
	}

	
}
