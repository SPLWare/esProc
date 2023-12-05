package com.scudata.expression.mfn;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.MemberFunction;

/**
 * ¶à²ãµÝ¹éÕ¹¿ª
 * x.nodes@r(Fi,...)
 * @author RunQian
 *
 */
public class Nodes extends MemberFunction {
	protected Object src;
	
	public boolean isLeftTypeMatch(Object obj) {
		return option != null && option.indexOf('r') != -1;
	}

	public void setDotLeftObject(Object obj) {
		src = obj;
	}

	public Object calculate(Context ctx) {
		String []fields = null;
		if (param != null) {
			fields = param.toStringArray("nodes", false);
		}
		
		Sequence result = new Sequence();
		if (src instanceof BaseRecord) {
			com.scudata.expression.mfn.sequence.Nodes.getAllSubs((BaseRecord)src, fields, result);
		} else if (src instanceof Sequence) {
			com.scudata.expression.mfn.sequence.Nodes.getAllSubs((Sequence)src, fields, result);
		}
		
		return result;
	}
}
