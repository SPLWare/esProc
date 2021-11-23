package com.scudata.lib.informix.function;

import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.expression.Node;

public class ImSaveFrag extends ImFunction {
	boolean bFresh = false;
	boolean bMultiCursor = false;
	
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}
	
	public Object doQuery( Object[] objs){
		try {
			String fileName = objs[0].toString().replace("\"", "");
			m_ifxConn.saveFragment(fileName);
			
			return m_ifxConn.listFrag();					
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
		
		return null;
	}
}
