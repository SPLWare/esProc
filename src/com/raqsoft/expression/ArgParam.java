package com.raqsoft.expression;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.ParamList;

/**
 * 参数节点，值不可被修改
 * @author RunQian
 *
 */
public class ArgParam extends Node {
	private Param param;

	public ArgParam(Param param) {
		this.param = param;
	}

	public Object calculate(Context ctx) {
		return param.getValue();
	}

	protected boolean containParam(String name) {
		return name.equals(param.getName());
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (resultList.get(param.getName()) == null) {
			resultList.addVariable(param.getName(), param.getValue());
		}
	}
}
