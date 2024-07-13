package com.scudata.expression;

import java.util.List;

import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
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
		if (left.containParam(name)) return true;
		return super.containParam(name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		left.getUsedParams(ctx, resultList);
		super.getUsedParams(ctx, resultList);
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		left.getUsedFields(ctx, resultList);
		super.getUsedFields(ctx, resultList);
	}

	protected void getUsedCells(List<INormalCell> resultList) {
		left.getUsedCells(resultList);
		super.getUsedCells(resultList);
	}
	
	/**
	 * 重置表达式，用于表达式缓存，多次执行使用不同的上下文，清除跟上下文有关的缓存信息
	 */
	public void reset() {
		getLeft().reset();
		super.reset();
	}
	
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		left = left.optimize(ctx);
		
		return this;
	}
	
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("{}" + mm.getMessage("function.invalidParam"));
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("{}" + mm.getMessage("function.missingParam"));
		}
		
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"{}\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
	}
	
	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		if (!left.canCalculateAll()) {
			return false;
		}
		
		return param.canCalculateAll();
	}
}
