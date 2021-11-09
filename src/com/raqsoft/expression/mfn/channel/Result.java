package com.raqsoft.expression.mfn.channel;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.ChannelFunction;

/**
 * 取管道的计算结果
 * ch.result()
 * @author RunQian
 *
 */
public class Result extends ChannelFunction {
	public Object calculate(Context ctx) {
		return channel.result();
	}
}
