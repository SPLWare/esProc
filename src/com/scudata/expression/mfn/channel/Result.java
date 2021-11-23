package com.scudata.expression.mfn.channel;

import com.scudata.dm.Context;
import com.scudata.expression.ChannelFunction;

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
