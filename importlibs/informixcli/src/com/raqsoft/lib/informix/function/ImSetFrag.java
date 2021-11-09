package com.raqsoft.lib.informix.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.informix.helper.Fragment;
import com.raqsoft.resources.EngineMessage;

public class ImSetFrag extends ImFunction {
	boolean bCursor = false;
	boolean bMultiCursor = false;
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object doQuery( Object[] objs){
		if (objs.length<2){
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifx_setflag" + mm.getMessage("function.invalidParam"));
		}
		Fragment frag = new Fragment();
		frag.setTableName(objs[0].toString());
		frag.setFieldName(objs[1].toString());
		
		String minVals = "kkk";
		for (int i=2; i<objs.length; i++){
			frag.addPartition(objs[i]);
			minVals += ","+objs[i].toString();
		}
		if (minVals.equals("kkk")){
			minVals = null;
		}else{
			minVals = minVals.replaceFirst("kkk,", "");
		}
		
		frag.setPartitionVal(minVals);
		m_ifxConn.setFrag(frag);
		if (frag.getPartitionCount()==0){
			return null;
		}else{
			return m_ifxConn.listFragment(frag);
		}
	}
}
