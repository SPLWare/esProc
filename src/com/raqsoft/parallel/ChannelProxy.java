package com.raqsoft.parallel;

import com.raqsoft.dm.IResource;
import com.raqsoft.dm.op.Channel;

public class ChannelProxy extends IProxy {
	private Channel channel;
		
	public ChannelProxy(Channel channel) {
		this.channel = channel;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public void close() {
		Object result = channel.result();
		if (result instanceof IResource) {
			((IResource)result).close();
		}
	}
}