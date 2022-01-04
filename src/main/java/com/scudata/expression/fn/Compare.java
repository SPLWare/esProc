package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 比较两个值的大小，左值大返回1，相等返回0，左值小返回-1
 * cmp(x,y) cmp(A,B) cmp(A) cmp(A;B)
 * @author RunQian
 *
 */
public class Compare extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cmp" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmp" + mm.getMessage("function.paramTypeError"));
			}
			
			return ((Sequence)result1).cmp0();
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmp" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cmp" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			Object result2 = sub2.getLeafExpression().calculate(ctx);
			
			if (result1 instanceof Record) {
				if (result2 instanceof Record) {
					return ((Record)result1).compare((Record)result2);
				}
			} else if (result1 instanceof Sequence) {
				if (param.getType() == IParam.Semicolon) {
					// cmp(A;B) A可以是子表的记录，主键数多于B
					Sequence a = (Sequence)result1;
					if (result2 instanceof Sequence) {
						Sequence b = (Sequence)result2;
						if (a.length() > b.length()) {
							return compare(a, b, b.length());
						} else {
							return a.cmp(b);
						}
					} else {
						if (a.length() != 0) {
							return Variant.compare(a.getMem(1), result2, true);
						} else {
							return -1;
						}
					}
				}

				if (result2 instanceof Sequence) {
					return ((Sequence)result1).cmp((Sequence)result2);
				} else if (result2 instanceof Number && ((Number)result2).intValue() == 0) {
					return ((Sequence)result1).cmp0();
				}
			}
			
			return Variant.compare(result1, result2, true);
		}
	}
	
	private static int compare(Sequence seq1, Sequence seq2, int len) {
		for (int i = 1; i <= len; ++i) {
			int cmp = Variant.compare(seq1.getMem(i), seq2.getMem(i), true);
			if (cmp != 0) return cmp;
		}
		
		return 0;
	}
}
