package com.scudata.expression.fn;

import java.util.UUID;

import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;

/**
 * 获得全系统唯一的字串
 * @author runqian
 *
 */
public class CreateUUID extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		return UUID.randomUUID().toString();
	}
}
