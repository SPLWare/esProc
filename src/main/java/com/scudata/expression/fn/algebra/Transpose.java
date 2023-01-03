package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 矩阵转置
 * @author bd
 *
 */
public class Transpose extends Function{
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("transpose" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("transpose" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("transpose" + mm.getMessage("function.paramTypeError"));
		}
		// 单纯用序列转置，不改变成员数据类型，但转置后矩阵一定会填满，缺失值用0d
		Sequence seq = (Sequence) result1;
		int rows = seq.length();
		int cols = 0;
		for (int r = 1; r <= rows; r++) {
			Object o = seq.get(r);
			if (o instanceof Sequence) {
				cols = Math.max(cols, ((Sequence) o).length());
			}
		}
		Double zero = Double.valueOf(0);
		if (cols == 0) {
			Sequence result = new Sequence(rows);
			// 一位序列，只需转成列式就行
			for (int r = 1; r <= rows; r++ ) {
				Object o = seq.get(r);
				Sequence sub = new Sequence(1);
				sub.add(o);
				result.add(sub);
			}
			return result;
		}
		Sequence result = new Sequence(cols);
		for (int c = 1; c <= cols; c++) {
			Sequence sub = new Sequence(rows);
			for (int r = 1; r <= rows; r++) {
				Object o = seq.get(r);
				if (o instanceof Sequence) {
					Sequence subSeq = (Sequence) o;
					if (subSeq.length() >= c) {
						o = subSeq.get(c);
					}
				}
				else if (c > 1) {
					o = zero;
				}
				sub.add(o);
			}
			result.add(sub);
		}
		return result;
	}
}
