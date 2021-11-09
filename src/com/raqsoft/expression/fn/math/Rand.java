package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

/**
 * rand(n)	返回小于n的随机整数，n省略则返回[0,1]区间的随机小数
	@s	设定随机数种子
 *
 */
public class Rand extends Function {

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {//返回[0,1]区间的随机小数
			return new Double(ctx.getRandom().nextDouble());
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				throw new RQException("rand" + mm.getMessage("function.paramTypeError"));
			}

			if (option == null || option.indexOf('s') == -1) {//返回小于n的随机整数
				int n = ((Number)obj).intValue();
				return new Integer(ctx.getRandom().nextInt(n));
			} else {//设定随机数种子
				long seed = ((Number)obj).longValue();
				ctx.getRandom(seed);
				return null;
			}
		} else {
			throw new RQException("rand" + mm.getMessage("function.invalidParam"));
		}
	}

	public Node optimize(Context ctx) {
		return this;
	}
}
