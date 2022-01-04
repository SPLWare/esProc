package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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
