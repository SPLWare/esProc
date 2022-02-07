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
 * 取出排号k的相应字节构成的单字节排号
 * k{a,b:c,d,e} 
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
		Object val = getLeft().calculate(ctx);
		if (!(val instanceof SerialBytes)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("{}" + mm.getMessage("function.paramTypeError"));
		}
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("{}" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.paramTypeError"));
			}
			
			int n = ((Number)obj).intValue();
			return ((SerialBytes)val).getByte(n);
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.invalidParam"));
			}
			
			Object s = sub0.getLeafExpression().calculate(ctx);
			if (!(s instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.paramTypeError"));
			}
			
			Object e = sub1.getLeafExpression().calculate(ctx);
			if (!(e instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.paramTypeError"));
			}
			
			int start = ((Number)s).intValue();
			int end  = ((Number)e).intValue();
			return ((SerialBytes)val).getBytes(start, end);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("{}" + mm.getMessage("function.invalidParam"));
		}
	}
}
