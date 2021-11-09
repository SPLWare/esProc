package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.sap.conn.jco.JCoTable;

public class ImCursor extends ImFunction {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		m_bCursor = true;
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		JCoTable bt = null;
		
		do{
			bt = doJcoTable(objs);
	        if (bt==null){
				Logger.error("getTable" + objs[1] + " false");
				m_rfcManager.close();
				throw new RQException("getTable" + objs[1] + " false");
			}
	        return new ImCursorImpl(bt, m_ctx);
		}while(false);
	}
}
