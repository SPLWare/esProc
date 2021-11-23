package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 两个向量之间的欧氏距离dis(A, B)
 * @author bd
 */
public class Distance extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("dis" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Sequence) {
				return o;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("dis" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("dis" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("dis" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				Matrix A = new Matrix((Sequence)o1);
				Matrix B = new Matrix((Sequence)o2);
				if (A.getCols() == 0 && A.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dis" + mm.getMessage("function.paramTypeError"));
				}
				else if (B.getCols() == 0 && B.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("dis" + mm.getMessage("function.paramTypeError"));
				}
				Matrix X = new Matrix(A.getRows(), B.getRows());
				double[][] xs = X.getArray();
				boolean manhattan = false;
				boolean standard = false;
				if (option != null) {
					if (option.indexOf('a') > -1) {
						manhattan = true;
					}
					if (option.indexOf('m') > -1) {
						standard = true;
					}
				}
				for (int r = 0; r < A.getRows(); r++) {
					for (int c = 0; c < B.getRows(); c++) {
						double res = 0;
						for (int i = 0; i < A.getCols(); i++) {
							if (manhattan) {
								res += Math.abs(A.get(r, i)-B.get(c, i));
							}
							else {
								res += Math.pow((A.get(r, i) - B.get(c, i)), 2);
							}
						}
						if (standard) {
							res = res / A.getCols();
						}
						if (manhattan) {
							xs[r][c] = res;
						}
						else {
							xs[r][c] = Math.sqrt(res);
						}
					}
				}
				return X.toSequence(option, false);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("dis" + mm.getMessage("function.paramTypeError"));
			}
		}
	}

}
