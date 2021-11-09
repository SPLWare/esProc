package com.raqsoft.lib.informix.function;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class ImListFrag extends ImFunction {
	boolean bCursor = false;
	boolean bMultiCursor = false;
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}
	
	public Object doQuery( Object[] objs){
		Table tbl = null;
		if (objs==null){
			tbl = m_ifxConn.listFrag();
		}else{
			tbl = m_ifxConn.listFrag(objs[0].toString());
		}
		
		return tbl;
	}
}
