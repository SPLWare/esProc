package com.scudata.expression.mfn.channel;

import com.scudata.dm.Context;
import com.scudata.expression.ChannelFunction;

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