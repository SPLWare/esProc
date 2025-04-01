package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 对角二维单位矩阵函数eye(n1,n2)
 * @author bd
 */
public class Eye extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eye" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Number) {
				int n = ((Number) o).intValue();
				return eye(n, n);
			}
			else if (o instanceof Sequence) {
				Sequence seq = (Sequence) o;
				int len = seq.length();
				if (len != 2) {
					if (len < 1 || len > 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("eye" + mm.getMessage("function.invalidParam"));
					}
					else {
						// 单值序列
						o = seq.get(1);
						if (o instanceof Number) {
							int n = ((Number) o).intValue();
							return eye(n, n);
						}
						else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("eye" + mm.getMessage("function.paramTypeError"));
						}
					}
				}
				Object o1 = seq.get(1);
				Object o2 = seq.get(2);
				if (o1 instanceof Number && o2 instanceof Number) {
					int rows = ((Number) o1).intValue();
					int cols = ((Number) o2).intValue();
					return eye(rows, cols);
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("eye" + mm.getMessage("function.paramTypeError"));
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eye" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2){
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eye" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Number && o2 instanceof Number) {
				int rows = ((Number) o1).intValue();
				int cols = ((Number) o2).intValue();
				return eye(rows, cols);
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("eye" + mm.getMessage("function.paramTypeError"));
			}
		}
		else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eye" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private final static Double ONE = Double.valueOf(1d);
	private final static Double ZERO = Double.valueOf(0d);
	protected Sequence eye(int rows, int cols) {
		if (rows < 1 || cols < 1) {
			return new Sequence();
		}
		Sequence result = new Sequence(rows);
        for (int i = 0; i < rows; i++) {
    		Sequence sub = new Sequence(cols);
            for (int j = 0; j < cols; j++) {
                if (i == j) {
                    sub.add(ONE);
                } else {
                    sub.add(ZERO);
                }
            }
            result.add(sub);
        }
		return result;
	}
}
