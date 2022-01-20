package com.scudata.lib.matrix;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;


/**
 * 向量或矩阵中计算求和
 * msum(A)，A为向量时结果为成员总和，A为矩阵或者多维矩阵时计算最外层矩阵行的成员总和，返回序列或者多维序列
 * msum(A, n)，计算第n层，n支持数列聚合多层
 * @author bd
 *
 */
public class MSum extends Function {
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("msum" + mm.getMessage("function.missingParam"));
		} else {
			Object oa = null;
			Object o2 = null;
			if (param.isLeaf()) {
				// 只有一个参数，msum(A), 相当于msum(A, 1)
				oa = param.getLeafExpression().calculate(ctx);
			}
			else if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("msum" + mm.getMessage("function.invalidParam"));
			}
			else {
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("msum" + mm.getMessage("function.invalidParam"));
				}
				oa = sub1.getLeafExpression().calculate(ctx);
				o2 = sub2.getLeafExpression().calculate(ctx);
			}
			boolean ifNull = option != null && option.contains("n");
			if (oa instanceof Sequence) {
				MulMatrix A = new MulMatrix((Sequence)oa);
				if (option != null && option.contains("a")) {
					// 全聚合，第2个维度参数无效
					return A.sumAll();
				}
				if (o2 instanceof Sequence) {
					// 第2个参数为序列时，需要为数列，数列中重复的数以及小于0或者大于多维序列层数的无效，也不报错
					Sequence nseq = ((Sequence) o2).id(null);
					int len = ((Sequence) o2).length();
					if (len != nseq.length()) {
						throw new RQException("msum: Demension vector members must be unique.");
					}
					Object result = sum(A, nseq, ifNull);
					if (result instanceof MulMatrix) {
						return ((MulMatrix) result).toSequence();
					}
					return result;
				}
				else if (o2 != null && !(o2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("msum" + mm.getMessage("function.paramTypeError"));
				}
				else {
					int b = 0;
					if (o2 instanceof Number) {
						b = ((Number) o2).intValue();
					}
					Object result = sum(A, b, ifNull);
					if (result instanceof MulMatrix) {
						result = ((MulMatrix) result).toSequence();
					}
					return result;
				}
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("msum" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	protected static Object sum(MulMatrix A, int level, boolean ifNull) {
		return A.sum(level, ifNull);
	}
	
	protected static Object sum(MulMatrix A, Sequence nseq, boolean ifNull) {
		int len = nseq.length();
		for(int i = 1; i <= len; i++) {
			Object o = nseq.get(i);
			if (o instanceof Number) {
				int level = ((Number) o).intValue() - i + 1;
				o = A.sum(level, ifNull);
				if (o instanceof MulMatrix) {
					A = (MulMatrix) o;
				}
				else {
					// 聚合结果为常数了，无法再聚合了，返回
					return o;
				}
			}
		}
		return A;
	}
}
