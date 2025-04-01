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
 * 全0矩阵函数zeros(n1,n2,...)
 * @author bd
 */
public class Zeros extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("zeros" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			Sequence nseq = null;
			if (o instanceof Number) {
				nseq = new Sequence(2);
				nseq.add(o);
				nseq.add(o);
			}
			else if (o instanceof Sequence) {
				nseq = (Sequence) o;
				if (nseq.length() < 2) {
					if (nseq.length() < 1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("zeros" + mm.getMessage("function.invalidParam"));
					}
					else {
						// 单值序列
						o = nseq.get(1);
						nseq = new Sequence(2);
						nseq.add(o);
						nseq.add(o);
					}
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("zeros" + mm.getMessage("function.paramTypeError"));
			}
			return zeros(nseq);
		} else {
			int size = param.getSubSize();
			Sequence nseq = new Sequence(size);
			for (int i = 0; i < size; i++) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("zeros" + mm.getMessage("function.invalidParam"));
				}
				Object o = sub.getLeafExpression().calculate(ctx);
				if (o instanceof Number) {
					nseq.add(o);
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("zeros" + mm.getMessage("function.paramTypeError"));
				}
			}
			return zeros(nseq);
		}
	}
	
	private final static Double ZERO = Double.valueOf(0d);
	protected Sequence zeros(Sequence nseq) {
		int n = 1;
		Object o = nseq.get(1);
		if (o instanceof Number) {
			n = ((Number) o).intValue();
		}
		Sequence result = new Sequence(n);
		int size = nseq.length();
		Sequence nseq2 = null;
		if (size > 1) {
			nseq2 = new Sequence(size - 1);
			for (int i = 2; i <= size; i++) {
				nseq2.add(nseq.get(i));
			}
		}
		for (int i = 1; i <= n; i++) {
			if (size > 1) {
				result.add(zeros(nseq2));
			}
			else {
				result.add(ZERO);
			}
		}
		return result;
	}
}
