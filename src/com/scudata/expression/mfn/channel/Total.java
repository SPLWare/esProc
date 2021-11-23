package com.scudata.expression.mfn.channel;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.ChannelFunction;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

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
