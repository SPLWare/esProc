package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Channel;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Push;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.parallel.ClusterChannel;
import com.scudata.parallel.ClusterCursor;
import com.scudata.resources.EngineMessage;

/**
 * channel() 创建管道
 * @author runqian
 *
 */
public class CreateChannel extends Function {
	public Node optimize(Context ctx) {
		return this;
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			return new Channel(ctx);
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof ClusterChannel) {
				return new ClusterChannel((ClusterChannel)obj, ctx);
			} else if (obj instanceof ClusterCursor) {
				return new ClusterChannel((ClusterCursor)obj, ctx);
			} else if (obj instanceof ICursor) {
				return ((ICursor)obj).newChannel(ctx, true);
			} else if (obj instanceof Operable) {
				Channel channel = new Channel(ctx);
				Push push = new Push(this, channel);
				((Operable)obj).addOperation(push, ctx);
				
				return channel;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("channel" + mm.getMessage("function.paramTypeError"));
			} else {
				return new Channel(ctx);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("channel" + mm.getMessage("function.invalidParam"));
		}
	}
}
