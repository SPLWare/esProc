package com.scudata.expression.fn;

import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.CSVariable;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 调用子程序
 * func(a,arg)在查询中定义完成子程序后，就可以在任意单元格中进行子程序的调用。  
 * @author runqian
 *
 */
public class Func extends Function {
	public class CallInfo {
		private INormalCell cell; // 定义函数时没有起名字，用函数所在的单元格引用
		private String fnName; // 定义函数时起了名字
		private Object []args;
		
		public CallInfo(INormalCell cell) {
			this.cell = cell;
		}
		
		public CallInfo(String fnName) {
			this.fnName = fnName;
		}
		
		public PgmCellSet getPgmCellSet() {
			return (PgmCellSet)cs;
		}
		
		public INormalCell getCell() {
			return cell;
		}

		public int getRow(){
			return cell.getRow();
		}
		
		public int getCol() {
			return cell.getCol();
		}
		
		public Object[] getArgs() {
			return args;
		}

		public void setArgs(Object[] args) {
			this.args = args;
		}

		public String getFnName() {
			return fnName;
		}
	}

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
	
	public Object calculate(Context ctx) {
		CallInfo callInfo = getCallInfo(ctx);
		PgmCellSet pcs = (PgmCellSet)cs;
		INormalCell cell = callInfo.getCell();
		Object []args = callInfo.getArgs();
		
		if (cell != null) {
			return pcs.executeFunc(cell.getRow(), cell.getCol(), args);
		} else {
			return pcs.executeFunc(callInfo.getFnName(), args);
		}
	}
	
	private CallInfo getCallInfo(Expression exp, Context ctx) {
		if(exp.getHome() instanceof CSVariable) {
			INormalCell cell = exp.calculateCell(ctx);
			return new CallInfo(cell);
		} else {
			return new CallInfo(exp.toString());
		}
	}
	
	// ide用来取调用信息进行单步跟踪
	public CallInfo getCallInfo(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("func" + mm.getMessage("function.missingParam"));
		}

		CallInfo callInfo;
		if (param.isLeaf()) {
			callInfo = getCallInfo(param.getLeafExpression(), ctx);
		} else {
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("func" + mm.getMessage("function.invalidParam"));
			}

			int size = param.getSubSize();
			Object []args = new Object[size - 1];
			callInfo = getCallInfo(sub0.getLeafExpression(), ctx);
			callInfo.setArgs(args);
			
			for (int i = 1; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub != null) {
					args[i - 1] = sub.getLeafExpression().calculate(ctx);
				}
			}
		}
		
		return callInfo;
	}
}
