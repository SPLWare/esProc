package com.scudata.lib.dynamodb.function;

import com.scudata.dm.Context;
import com.scudata.expression.Node;

// conn.dyna_close()
public class ImClose extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (m_db!=null) {
			m_db.close();
			m_db = null;
		}
		
		return null;
	}
}
