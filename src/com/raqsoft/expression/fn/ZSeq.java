package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 计算某个成员分到哪个子集合
 * z(i,n) 自然数集合分成子集合长度为k的几份，计算第i个成员在第几个子集合中
 * @author runqian
 *
 */
public class ZSeq extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("z" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("z" + mm.getMessage("function.invalidParam"));
			}
			
			Object i = sub0.getLeafExpression().calculate(ctx);
			Object k = sub1.getLeafExpression().calculate(ctx);
			
			if (!(i instanceof Number) || !(k instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("z" + mm.getMessage("function.paramTypeError"));
			}
			
			int g = (((Number)i).intValue() - 1) / ((Number)k).intValue() + 1;
			return new Integer(g);
		} else if (size == 3) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			IParam sub2 = param.getSub(2);
			if (sub0 == null || sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("z" + mm.getMessage("function.invalidParam"));
			}
			
			Object io = sub0.getLeafExpression().calculate(ctx);
			Object ko = sub1.getLeafExpression().calculate(ctx);
			Object no = sub2.getLeafExpression().calculate(ctx);
			
			if (!(io instanceof Number) || !(ko instanceof Number) || !(no instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("z" + mm.getMessage("function.paramTypeError"));
			}
			
			int i = ((Number)io).intValue();
			int k = ((Number)ko).intValue();
			int n = ((Number)no).intValue();
			
			int count = n / k;
			if (count == 0) {
				return i;
			}
			
			int mod = n % k;
			if (mod == 0) {
				return new Integer((i - 1) / count + 1);
			} else {
				int n1 = count * mod + mod;
				if (i > n1) {
					return new Integer((i - n1 - 1) / count + 1 + mod);
				} else {
					return new Integer((i - 1) / (count + 1) + 1);
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("z" + mm.getMessage("function.invalidParam"));
		}
	}
}
