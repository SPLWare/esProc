package com.scudata.expression;

import com.scudata.dm.op.Channel;

/**
 * 管道成员函数基类
 * ch.f()
 * @author RunQian
 *
 */
public abstract class ChannelFunction extends MemberFunction {
	protected Channel channel;

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Channel;
	}
	
	public void setDotLeftObject(Object obj) {
		channel = (Channel)obj;
	}
}