package com.scudata.expression.fn;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.Command;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
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
	// 一下用于优化m选项
	private Expression resultExp; // 此表达式为func函数体内的表达式
	private Expression []paramExps; // 参数表达式数组
	private INormalCell funcCell; // func所在的单元格
	//private String []argNames = null; // 参数名数组
	
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

	public void setParameter(ICellSet cs, Context ctx, String strParam) {
		super.setParameter(cs, ctx, strParam);
		
		if (option != null && option.indexOf('m') != -1) {
			PgmCellSet pcs = (PgmCellSet)cs;
			Expression fnExp = null;
			
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("func" + mm.getMessage("function.missingParam"));
			} else if (param.isLeaf()) {
				fnExp = param.getLeafExpression();
			} else {
				IParam sub0 = param.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("func" + mm.getMessage("function.invalidParam"));
				}

				fnExp = sub0.getLeafExpression();
				int size = param.getSubSize();
				paramExps = new Expression[size - 1];
				
				for (int i = 1; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub != null) {
						paramExps[i - 1] = sub.getLeafExpression();
					}
				}
			}

			if(fnExp.getHome() instanceof CSVariable) {
				funcCell = fnExp.calculateCell(ctx);
			} else {
				return;
				//String fnName = fnExp.toString();
				//PgmCellSet.FuncInfo fi = pcs.getFuncInfo(fnName);
				//funcCell = fi.getCell();
				//argNames = fi.getArgNames();
			}
			
			int row = funcCell.getRow();
			int col = funcCell.getCol();
			int endRow = pcs.getCodeBlockEndRow(row, col);
			int colCount = pcs.getColCount();
			
			// 只优化函数体内有一个表达式的情况
			
			for (int r = row; r <= endRow; ++r) {
				for (int c = col + 1; c <= colCount; ++c) {
					PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
					if (cell.isBlankCell()) {
						continue;
					} else if (resultExp != null) {
						resultExp = null;
						return;
					} else if (cell.isCommandCell()) {
						Command cmd = cell.getCommand();
						if (cmd.getType() == Command.RETURN) {
							IParam resultParam = cmd.getParam(pcs, ctx);
							if (resultParam != null && resultParam.isLeaf()) {
								resultExp = resultParam.getLeafExpression();
							} else {
								resultExp = null;
								return;
							}
						} else {
							resultExp = null;
							return;
						}
					} else if (cell.isCalculableCell()) {
						resultExp = cell.getExpression();
					} else {
						resultExp = null;
						return;
					}
				}
			}
		}
	}
	
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
	
	public Object calculate(Context ctx) {
		// 对m选项进行优化
		if (resultExp != null) {
			// 设置参数值
			if (paramExps != null) {
				int paramRow = funcCell.getRow();
				int paramCol = funcCell.getCol();
				int colCount = cs.getColCount();
				for (int i = 0, pcount = paramExps.length; i < pcount; ++i) {
					Object value = paramExps[i].calculate(ctx);
					cs.getCell(paramRow, paramCol).setValue(value);
					if (paramCol < colCount) {
						paramCol++;
					} else {
						break;
					}
				}
			}
			
			return resultExp.calculate(ctx);
		}
		
		CallInfo callInfo = getCallInfo(ctx);
		PgmCellSet pcs = (PgmCellSet)cs;
		INormalCell cell = callInfo.getCell();
		Object []args = callInfo.getArgs();
		
		if (cell != null) {
			return pcs.executeFunc(cell.getRow(), cell.getCol(), args, option);
		} else {
			return pcs.executeFunc(callInfo.getFnName(), args, option);
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
