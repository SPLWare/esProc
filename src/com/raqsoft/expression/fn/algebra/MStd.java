package com.raqsoft.expression.fn.algebra;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;


/**
 * 归一化数据，按向量返回 A 中数据的 z 值（中心为 0、标准差为 1）
 * mstd(A)，计算最外层矩阵的成员累积和，返回多维序列
 * mstd(A, n)，计算第n层，n暂时不支持数列
 * @author bd
 *
 */
public class MStd extends Function {
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mstd" + mm.getMessage("function.missingParam"));
		} else {
			Object oa = null;
			Object o2 = null;
			if (param.isLeaf()) {
				// 只有一个参数，mstd(A), 按照A大小不等于 1的第一个数组维度进行运算
				oa = param.getLeafExpression().calculate(ctx);
			}
			else if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mstd" + mm.getMessage("function.invalidParam"));
			}
			else {
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mstd" + mm.getMessage("function.invalidParam"));
				}
				oa = sub1.getLeafExpression().calculate(ctx);
				o2 = sub2.getLeafExpression().calculate(ctx);
			}
			// 是否用样本n-1计算方差，默认不用
			boolean s = option != null && option.contains("s");
			if (oa instanceof Sequence) {
				MulMatrix A = new MulMatrix((Sequence)oa);
				if (option != null && option.contains("a")) {
					// 全聚合，第2个维度参数无效
					double sum = A.sumAll();
					double d = A.countAll();
					double avg = sum/d;
					double sd = A.sd(avg);
					if (s) {
						return Math.sqrt(sd/(d-1));
					}
					return Math.sqrt(sd/d);
				}
				if (o2 != null && !(o2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mstd" + mm.getMessage("function.paramTypeError"));
				}
				else {
					int b = 0;
					if (o2 instanceof Number) {
						b = ((Number) o2).intValue();
					}
					Object result = std(A, b, s);
					if (result instanceof MulMatrix) {
						return ((MulMatrix) result).toSequence();
					}
					return result;
				}
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("mstd" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	protected static Object std(MulMatrix A, int level, boolean s) {
		return A.std(level, s);
	}
}
