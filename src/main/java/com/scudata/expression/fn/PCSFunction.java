package com.scudata.expression.fn;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;

/**
 * 程序网里定义的函数的调用 fn(arg)fn为程序网中定义的函数的名字
 * 
 * @author runqian
 *
 */
public class PCSFunction extends Function {
	private PgmCellSet.FuncInfo funcInfo;

	public PCSFunction(PgmCellSet.FuncInfo funcInfo) {
		this.funcInfo = funcInfo;
	}

	public Node optimize(Context ctx) {
		if (param != null)
			param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		Object[] args = null;
		if (param != null) {
			if (param.isLeaf()) {
				Object val = param.getLeafExpression().calculate(ctx);
				args = new Object[] { val };
			} else {
				int size = param.getSubSize();
				args = new Object[size];

				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub != null) {
						args[i] = sub.getLeafExpression().calculate(ctx);
					}
				}
			}
		}

		PgmCellSet pcs = (PgmCellSet) cs;
		return pcs.executeFunc(funcInfo, args, option);
	}

	/**
	 * 取函数调用信息
	 * 
	 * @return
	 */
	public PgmCellSet.FuncInfo getFuncInfo() {
		return funcInfo;
	}

	/**
	 * 
	 * @param ctx
	 * @return
	 */
	public Object[] prepareArgs(Context ctx) {
		Object[] args = null;
		if (param != null) {
			if (param.isLeaf()) {
				Object val = param.getLeafExpression().calculate(ctx);
				args = new Object[] { val };
			} else {
				int size = param.getSubSize();
				args = new Object[size];

				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub != null) {
						args[i] = sub.getLeafExpression().calculate(ctx);
					}
				}
			}
		}
		return args;
	}
}
