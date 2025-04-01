package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;


/**
 * 向量或矩阵中计算累积求和，@z选项为逆向累积
 * mcumsum(A)，计算最外层矩阵的成员累积和，返回多维序列
 * mcumsum(A, n)，计算第n层，不支持数列
 * @author bd
 *
 */
public class MCumsum extends Function {
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mcumsum" + mm.getMessage("function.missingParam"));
		} else {
			Object oa = null;
			Object o2 = null;
			if (param.isLeaf()) {
				// 只有一个参数，mcumsum(A), 相当于mcumsum(A, 1)
				oa = param.getLeafExpression().calculate(ctx);
			}
			else if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mcumsum" + mm.getMessage("function.invalidParam"));
			}
			else {
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mcumsum" + mm.getMessage("function.invalidParam"));
				}
				oa = sub1.getLeafExpression().calculate(ctx);
				o2 = sub2.getLeafExpression().calculate(ctx);
			}
			boolean reverse = option != null && option.contains("z");
			if (oa instanceof Sequence) {
				MulMatrix A = new MulMatrix((Sequence)oa);
				if (o2 != null && !(o2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mcumsum" + mm.getMessage("function.paramTypeError"));
				}
				else {
					int b = 0;
					if (o2 instanceof Number) {
						b = ((Number) o2).intValue();
					}
					MulMatrix result = cumsum(A, b, reverse);
					return result.toSequence();
				}
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("mcumsum" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	protected static MulMatrix cumsum(MulMatrix A, int level, boolean reverse) {
		return A.cumsum(level, reverse);
	}
}
