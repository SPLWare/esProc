package com.raqsoft.dw;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Param;
import com.raqsoft.expression.Node;
import com.raqsoft.util.Variant;

class NodeFilter extends IFilter {
	private Node node;
	private Param param;
	private Context ctx;

	public NodeFilter(ColumnMetaData column, int priority, Node node, Context ctx) {
		super(column, priority);
		
		this.node = node;
		this.ctx = ctx;
		
		param = ctx.getParam(column.getColName());
		if (param == null) {
			param = new Param(column.getColName(), Param.VAR, null);
			ctx.addParam(param);
		}
	}
	
	public boolean match(Object value) {
		param.setValue(value);
		return Variant.isTrue(node.calculate(ctx));
	}
	
	public boolean match(Object minValue, Object maxValue) {
		if (Variant.isEquals(minValue, maxValue)) {
			return match(minValue);
		} else {
			return true;
		}
	}
}