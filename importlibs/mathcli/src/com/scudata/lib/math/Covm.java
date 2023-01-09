package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.resources.EngineMessage;

/**
 * 协方差矩阵处理
 * @author bd
 *
 */
public class Covm extends Function{
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("covm" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("covm" + mm.getMessage("function.paramTypeError"));
			}
			// edited by bd, 2021.11.17, 在dis函数中，单层序列认为是横向量
			Sequence s1 = (Sequence) result1;
			Matrix A = new Matrix(s1);
			
			Object o11 = s1.length() > 0 ? s1.get(1) : null;
			if (!(o11 instanceof Sequence)) {
				// A为单序列定义的向量，转成横向量
				A = A.transpose();
			}
			if (A.getCols() == 0 || A.getRows() == 0) {
				return new Sequence(0);
			}
			Matrix X = A.covm();
			if (X == null) {
				return null;
			}
			return X.toSequence(option, true);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("covm" + mm.getMessage("function.invalidParam"));
		}
	}
}
