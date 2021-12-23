package com.scudata.lib.dynamodb.function;

import java.util.ArrayList;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;

public class ImFunction extends Function {
	protected Context m_ctx;

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			return doQuery(null);
		}

		Object[] objs = null;
		if (param.isLeaf()) {
			objs = new Object[1];
			objs[0] = param.getLeafExpression().calculate(ctx);
			return doQuery(objs);
		}

		int size = param.getSubSize();
		objs = new Object[size];
		IParam subPM = null;
		ArrayList<Expression> ls = new ArrayList<Expression>();

		for (int i = 0; i < size; i++) {
			if (param.getSub(i) == null) {
				objs[i] = null;
				continue;
			}

			subPM = param.getSub(i);
			if (subPM.isLeaf()) {
				objs[i] = subPM.getLeafExpression().calculate(ctx);
			} else {
				ls.clear();
				subPM.getAllLeafExpression(ls);
				Object[] os = new Object[ls.size()];
				for(int j=0; j<ls.size(); j++){
					if (ls.get(j)!=null){
						os[j] = ls.get(j).calculate(ctx);
					}else{
						os[j] = null;
					}
				}
				objs[i] = os;
			}
		}

		return doQuery(objs);
	}

	protected Object doQuery(Object[] objs) {
		return null;
	}
}
