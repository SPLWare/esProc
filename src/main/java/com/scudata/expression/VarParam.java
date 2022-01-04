package com.scudata.expression;

import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.util.Variant;

/**
 * 变量节点
 * @author RunQian
 *
 */
public class VarParam extends Node {
	private Param param;

	public VarParam(Param param) {
		this.param = param;
	}

	public Object calculate(Context ctx) {
		return param.getValue();
	}

	public Object assign(Object value, Context ctx) {
		param.setValue(value);
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		Object result = Variant.add(param.getValue(), value);
		param.setValue(result);
		return result;
	}

	protected boolean containParam(String name) {
		return name.equals(param.getName());
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (resultList.get(param.getName()) == null) {
			resultList.addVariable(param.getName(), param.getValue());
		}
	}

	public byte calcExpValueType(Context ctx) {
		Object val = param.getValue();
		if (val instanceof DBObject) {
			return Expression.TYPE_DB;
		} else if (val instanceof FileObject) {
			return Expression.TYPE_FILE;
		} else {
			return Expression.TYPE_OTHER;
		}
	}
}
