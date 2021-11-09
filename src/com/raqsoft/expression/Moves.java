package com.raqsoft.expression;

import java.util.List;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.common.IntArrayList;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ParamList;
import com.raqsoft.dm.SerialBytes;
import com.raqsoft.resources.EngineMessage;

/**
 * 取出排号k的相应字节构成的单字节排号
 * k{a,b:c,d,e} 
 * @author RunQian
 *
 */
public class Moves extends Function {
	private Node left;
	private int []seqs;
	
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

	public int[] getSeqs(Context ctx) {
		if (seqs != null) {
			return seqs;
		}
		
		IParam param = this.param;
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
			if (n < 1 || n > 8) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.invalidParam"));
			}
			
			seqs = new int[] {n};
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.invalidParam"));
			}
			
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
			if (start < 1 || start > end || end > 8) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.invalidParam"));
			}
			
			seqs = new int[end - start + 1];
			for (int i = 0; start <= end; ++start, ++i) {
				seqs[i] = start;
			}
		} else {
			int size = param.getSubSize();
			IntArrayList list = new IntArrayList(8);
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("{}" + mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("{}" + mm.getMessage("function.paramTypeError"));
					}
					
					int n = ((Number)obj).intValue();
					if (n < 1 || n > 8) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("{}" + mm.getMessage("function.invalidParam"));
					}
					
					list.addInt(n);
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("{}" + mm.getMessage("function.invalidParam"));
					}
					
					IParam sub0 = sub.getSub(0);
					IParam sub1 = sub.getSub(1);
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
					if (start < 1 || start > end || end > 8) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("{}" + mm.getMessage("function.invalidParam"));
					}
					
					for (; start <= end; ++start) {
						list.addInt(start);
					}
				}
			}
			
			if (list.size() > 8) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("{}" + mm.getMessage("function.invalidParam"));
			}
			
			seqs = list.toIntArray();
		}
		
		return seqs;
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
