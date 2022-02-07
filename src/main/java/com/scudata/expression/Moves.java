package com.scudata.expression;

import java.util.List;

import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.SerialBytes;
import com.scudata.resources.EngineMessage;

/**
 * 取出附表记录返回序表
 * T{x:C,……} 
 * @author RunQian
 *
 */
public class Moves extends Function {
	private Node left;
	
	public Moves() {
		priority = PRI_SUF;
	}

	public void setLeft(Node node) {
		left = node;
	}

	public Node getLeft() {
		return left;
	}

	protected boolean containParam(String name) {
		if (left != null && left.containParam(name)) return true;
		return super.containParam(name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (left != null) left.getUsedParams(ctx, resultList);
		super.getUsedParams(ctx, resultList);
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		if (left != null) left.getUsedFields(ctx, resultList);
		super.getUsedFields(ctx, resultList);
	}

	protected void getUsedCells(List<INormalCell> resultList) {
		if (left != null) left.getUsedCells(resultList);
		super.getUsedCells(resultList);
	}
	
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		if (left != null) {
			left = left.optimize(ctx);
		}
		
		return this;
	}
	
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("{}" + mm.getMessage("function.invalidParam"));
	}
}
