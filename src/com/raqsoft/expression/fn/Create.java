package com.raqsoft.expression.fn;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;
 
/**
 * create(Fi,...)产生以Fi,…为字段的空序表
 * @author runqian
 *
 */
public class Create extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("create" + mm.getMessage("function.missingParam"));
		}

		Expression []exps = getParamExpressions("create", true);
		int size = exps.length;

		String []names = new String[size];
		ArrayList<String> pkList = null;
		for (int i = 0; i < size; ++i) {
			if (exps[i] != null) {
				String name = exps[i].getIdentifierName();
				if (name != null && name.length() > 0 && name.charAt(0) == '#') {
					name = name.substring(1);
					if (pkList == null) {
						pkList = new ArrayList<String>();
					}
					
					pkList.add(name);
				}
				
				names[i] = name;
			}
		}

		Table table = new Table(names);
		if (pkList != null) {
			int count = pkList.size();
			String []pkNames = new String[count];
			pkList.toArray(pkNames);
			table.setPrimary(pkNames);
		}
		
		return table;
	}
}
