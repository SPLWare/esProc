package com.scudata.dw;

import java.util.ArrayList;
import java.util.List;

import com.scudata.dm.Context;
import com.scudata.dm.Param;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.util.Variant;

public class UnknownNodeFilter extends IFilter {
	private Node node;
	private Param param;
	private Context ctx;

	public UnknownNodeFilter(ColPhyTable table, Node node, Context ctx) {
		this.node = node;
		this.ctx = ctx;
		this.exp = new Expression(node);
		init(table, node, ctx);
	}
	
	public UnknownNodeFilter(ColPhyTable table, Expression exp, Context ctx) {
		this.exp = exp;
		this.ctx = ctx;
		init(table, exp.getHome(), ctx);
	}
	
	private void init(ColPhyTable table, Node node, Context ctx) {
		List<String> resultList = new ArrayList<String>();
		node.getUsedFields(ctx, resultList);
		int size = resultList.size();
		if (size > 0) {
			columns = new ArrayList<ColumnMetaData>(size);
			for (String name : resultList) {
				ColumnMetaData col = table.getColumn(name);
				if (col != null) {
					columns.add(col);
				}
			}
			colCount = columns.size();
		}
		priority = Integer.MAX_VALUE;
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
	
	public Node getNode() {
		return node;
	}
	
	public void setNode(Node node) {
		this.node = node;
	}
	
	public Param getParam() {
		return param;
	}

	public Context getCtx() {
		return ctx;
	}
	
	public List<ColumnMetaData> getColumns() {
		return columns;
	}
}
