package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;


/**
 * 归一化数据，按向量返回 A 中数据的 z 值（中心为 0、标准差为 1）
 * mnorm(A)，按照A大小不等于 1的第一个数组维度进行运算
 * mnorm(A, n)，计算第n层维度，不支持数列
 * @author bd
 *
 */
public class MNormalize extends Function {
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mnorm" + mm.getMessage("function.missingParam"));
		} else {
			Object oa = null;
			Object o2 = null;
			if (param.isLeaf()) {
				// 只有一个参数，mnorm(A), 按照A大小不等于 1的第一个数组维度进行运算
				oa = param.getLeafExpression().calculate(ctx);
			}
			else if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mnorm" + mm.getMessage("function.invalidParam"));
			}
			else {
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mnorm" + mm.getMessage("function.invalidParam"));
				}
				oa = sub1.getLeafExpression().calculate(ctx);
				o2 = sub2.getLeafExpression().calculate(ctx);
			}
			// 是否用样本n-1计算方差，默认不用
			boolean s = option != null && option.contains("s");
			if (oa instanceof Sequence) {
				MulMatrix A = new MulMatrix((Sequence)oa);
				if (o2 != null && !(o2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mnorm" + mm.getMessage("function.paramTypeError"));
				}
				else {
					int b = 0;
					if (o2 instanceof Number) {
						b = ((Number) o2).intValue();
					}
					MulMatrix result = normalize(A, b, s);
					return result.toSequence();
				}
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("mnorm" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	protected static MulMatrix normalize(MulMatrix A, int level, boolean s) {
		return A.normalize(level, s);
	}
}
