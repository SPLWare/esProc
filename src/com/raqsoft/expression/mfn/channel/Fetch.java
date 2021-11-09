package com.raqsoft.expression.mfn.channel;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.ChannelFunction;

/**
 * 为管道定义保留管道当前数据作为结果集的运算
 * ch.fetch()
 * @author RunQian
 *
 */
public class Fetch extends ChannelFunction {
	public Object calculate(Context ctx) {
		return channel.fetch();
	}
}