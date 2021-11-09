package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.cursor.MultipathCursors;
import com.raqsoft.dm.op.Channel;
import com.raqsoft.dm.op.MultipathChannel;
import com.raqsoft.dm.op.Operable;
import com.raqsoft.dm.op.Push;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.parallel.ClusterChannel;
import com.raqsoft.parallel.ClusterCursor;
import com.raqsoft.resources.EngineMessage;

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
			} else if (obj instanceof MultipathCursors) {
				return new MultipathChannel(ctx, (MultipathCursors)obj);
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
