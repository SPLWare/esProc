package com.scudata.expression.mfn.channel;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.ChannelFunction;
import com.scudata.resources.EngineMessage;

/**
 * 为管道定义保留管道当前数据作为结果集的运算
 * ch.fetch()
 * @author RunQian
 *
 */
public class Fetch extends ChannelFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return channel.fetch();
		} else if (param.isLeaf()) {
			FileObject fo;
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				fo = new FileObject((String)obj);
			} else if (obj instanceof FileObject) {
				fo = (FileObject)obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fetch" + mm.getMessage("function.paramTypeError"));
			}
			
			return channel.fetch(fo);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fetch" + mm.getMessage("function.invalidParam"));
		}
	}
}