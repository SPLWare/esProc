package com.raqsoft.expression;

import java.util.List;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ParamList;

/**
 * 表达式里运算符节点基类
 * @author RunQian
 *
 */
public abstract class Operator extends Node {
	protected Node left; // 左节点
	protected Node right; // 右节点

	public void setLeft(Node node) {
		this.left = node;
	}

	public void setRight(Node node) {
		this.right = node;
	}

	public Node getLeft() {
		return this.left;
	}

	public Node getRight() {
		return this.right;
	}

	protected boolean containParam(String name) {
		if (left != null && left.containParam(name)) return true;
		if (right != null && right.containParam(name)) return true;
		return false;
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (left != null) left.getUsedParams(ctx, resultList);
		if (right != null) right.getUsedParams(ctx, resultList);
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		if (left != null) left.getUsedFields(ctx, resultList);
		if (right != null) right.getUsedFields(ctx, resultList);
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
		if (left != null) left.getUsedCells(resultList);
		if (right != null) right.getUsedCells(resultList);
	}

	public Node optimize(Context ctx) {
		if (left != null) left = left.optimize(ctx);
		if (right != null) right = right.optimize(ctx);

		if (left instanceof Constant && right instanceof Constant) {
			return new Constant(calculate(ctx));
		} else {
			return this;
		}
	}
}
