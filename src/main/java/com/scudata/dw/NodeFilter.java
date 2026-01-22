package com.scudata.dw;

import java.util.ArrayList;

import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Param;
import com.scudata.dm.Record;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.util.Variant;

public class NodeFilter extends IFilter {
	private Node node;
	private Param param;
	private Context ctx;
	private Record cur;
	
	public NodeFilter(ColumnMetaData column, int priority, Node node, Context ctx) {
		super(column, priority);
		
		this.node = node;
		this.exp = new Expression(node);
		this.ctx = ctx;
		
		param = ctx.getParam(column.getColName());
		if (param == null) {
			param = new Param(column.getColName(), Param.VAR, null);
			ctx.addParam(param);
		}
		
		if (hasCurrentElement(node)) {
			DataStruct ds = new DataStruct(new String[]{column.getColName()});
			cur = new Record(ds);
		}
	}
	
	public boolean match(Object value) {
		param.setValue(value);
		if (cur != null) {
			cur.set(0, value);
			ComputeStack stack = ctx.getComputeStack();
			stack.push(cur);
			boolean b;
			try {
				b = Variant.isTrue(node.calculate(ctx));
			} finally {
				stack.pop();
			}
			return b;
		} else {
			return Variant.isTrue(node.calculate(ctx));
		}
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
	
	private boolean hasCurrentElement(Node node) {
		if (node == null) {
			return false;
		}
		if (node instanceof CurrentElement) {
			return true;
		}
		
		if (hasCurrentElement(node.getLeft())) {
			return true;
		}
		if (hasCurrentElement(node.getRight())) {
			return true;
		}
		if (node instanceof Function) {
			IParam param = ((Function)node).getParam();
			if (param != null) {
				if (param.isLeaf()) {
					Expression exp = param.getLeafExpression();
					if (exp != null) {
						return hasCurrentElement(exp.getHome());
					}
				} else {
					ArrayList<Expression> list = new ArrayList<Expression>();
					param.getAllLeafExpression(list);
					for (Expression exp : list) {
						if (exp != null) {
							if (hasCurrentElement(exp.getHome())) {
								return true;
							}
						}
					}
				}
			}
		}
		
		return false;
	}
}