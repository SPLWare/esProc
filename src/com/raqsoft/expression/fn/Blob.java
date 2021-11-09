package com.raqsoft.expression.fn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.ObjectCache;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * blob数据与数列互换，每字节对应一个成员
 * blob(b)
 * @author RunQian
 *
 */
public class Blob extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("blob" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return toBlob((Sequence)obj);
			} else if (obj instanceof byte[]) {
				return toSequence((byte[])obj);
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("blob" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private static Sequence toSequence(byte []bytes) {
		int len = bytes.length;
		if (len == 0) {
			return null;
		}
		
		Sequence seq = new Sequence(len);
		for (byte b : bytes) {
			seq.add(ObjectCache.getInteger(b & 0xff));
		}
		
		return seq;
	}
	
	private static byte[] toBlob(Sequence seq) {
		int len = seq.length();
		if (len == 0) {
			return null;
		}
		
		byte []bytes = new byte[len];
		for (int i = 1; i <= len; ++i) {
			Object obj = seq.getMem(i);
			if (obj instanceof Number) {
				bytes[i - 1] = ((Number)obj).byteValue();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntSeries"));
			}
		}
		
		return bytes;
	}
}
