package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 两个矩阵或者向量相乘
 * @author bd
 */
public class Mul extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mul" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Sequence) {
				return o;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mul" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mul" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mul" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			boolean ifva = o1 instanceof Sequence && ((Sequence) o1).length() > 0 && ((Sequence) o1).get(1) instanceof Number;
			boolean ifvb = o2 instanceof Sequence && ((Sequence) o2).length() > 0 && ((Sequence) o2).get(1) instanceof Number;
			if (ifvb) option = option == null ? "v" : option+"v";
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				Matrix A = new Matrix((Sequence)o1);
				Matrix B = new Matrix((Sequence)o2);
				if (A.getCols() == 0 || A.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mul" + mm.getMessage("function.paramTypeError"));
				}
				else if (B.getCols() == 0 || B.getRows() == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mul" + mm.getMessage("function.paramTypeError"));
				}
				if (ifva && ifvb && A.getCols() != B.getCols()) {
					A = A.transpose();
				}
				else if (A.getCols() != B.getRows() && ifvb ) {
					B = B.transpose();
				}
				Matrix X = A.times(B);
				if (X == null) {
					return null;
				}
				return X.toSequence(option, false);
			}
			else if (o1 instanceof Sequence || o2 instanceof Sequence) {
				Sequence seq = null;
				double mul = 1;
				if (o1 instanceof Sequence) {
					if (ifva) option = option == null ? "v" : option+"v";
					seq = (Sequence) o1;
					if (o2 instanceof Number) {
						mul = ((Number) o2).doubleValue();
					}
					else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("mul" + mm.getMessage("function.paramTypeError"));
					}
				}
				else {
					seq = (Sequence) o2;
					if (o1 instanceof Number) {
						mul = ((Number) o1).doubleValue();
					}
					else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("mul" + mm.getMessage("function.paramTypeError"));
					}
				}
				Matrix A = new Matrix(seq);
				double[][] vs = A.getArray();
				int rows = vs.length;
				int cols = A.getCols();
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						vs[r][c] *= mul;
					}
				}
				return A.toSequence(option, false);
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mul" + mm.getMessage("function.paramTypeError"));
			}
		}
	}

}
