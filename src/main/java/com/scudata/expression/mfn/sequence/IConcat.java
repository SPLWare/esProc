package com.scudata.expression.mfn.sequence;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.HashLinkSet;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 以分隔符连接序列中的成员成为字符串，处理子序列，去重
 * A.iconcat(d)
 * @author RunQian
 *
 */
public class IConcat extends SequenceFunction {
	public Object calculate(Context ctx) {
		String sep = "";
		if (param != null) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				sep = (String)obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("iconcat" + mm.getMessage("function.paramTypeError"));
			}
		}

		int len = srcSequence.length();
		HashLinkSet set = new HashLinkSet(len);
		set.putAll(srcSequence.getMems());
		IArray array = set.getElementArray();
		Sequence sequence = new Sequence(array);
		return sequence.toString(sep, option);
	}
}
