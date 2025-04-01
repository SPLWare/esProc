package com.scudata.expression.fn.algebra;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 矩阵求逆inverse(A)求逆，只有方阵满秩时有逆矩阵, inverse@p(A)伪逆矩阵，所有矩阵A都有伪逆矩阵B，满足ABA=B，BAB=A，无解时用最小二乘法
 * @author bd
 *
 */
public class Inverse extends Function{
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inverse" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inverse" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inverse" + mm.getMessage("function.paramTypeError"));
		}
		Matrix A = new Matrix((Sequence) result1);
		if (A.getCols() == 0 || A.getRows() == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inverse" + mm.getMessage("function.paramTypeError"));
		}
		Matrix X = null;
		boolean pseudo = false;
		boolean auto = false;
		if (option != null) {
			if (option.indexOf('a') > -1) {
				auto = true;
			}
			else if (option.indexOf('p') > -1) {
				pseudo = true;
			}
		}
		try {
			if (pseudo) {
				X = A.pseudoinverse();
			}
			else {
				X = A.inverse();
			}
		}
		catch (Exception e) {
			if (auto) {
				// A无法求逆时，求伪逆矩阵
				X = A.pseudoinverse();
			}
			else {
				Logger.warn("inverse error: " + e.getMessage());
				return null;
			}
		}
		if (X == null) {
			return null;
		}
		return X.toSequence(option, false);
	}
}
