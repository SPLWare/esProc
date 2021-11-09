package com.raqsoft.expression.mfn.channel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ChannelFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;

/**
 * 为管道定义汇总结果集运算
 * ch.total(y,…)
 * @author RunQian
 *
 */
public class Total extends ChannelFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("total" + mm.getMessage("function.missingParam"));
		}
		
		Expression []exps = param.toArray("total", false);
		channel.total(exps);
		return channel;
	}
}
