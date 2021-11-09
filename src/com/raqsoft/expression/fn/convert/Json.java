package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.JSONUtil;

/**
 * json(x) 当x是json格式串时，将x解析成序表返回；当x是记录或序列时，解析成json格式串返回。
 * @author runqian
 *
 */
public class Json extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("json" + mm.getMessage("function.invalidParam"));
		}

		Object val = param.getLeafExpression().calculate(ctx);
		if (val == null) {
			return null;
		} else if (val instanceof String) {
			char[] chars = ((String)val).toCharArray();
			return JSONUtil.parseJSON(chars, 0, chars.length - 1);
		} else if (val instanceof Sequence) {
			return JSONUtil.toJSON((Sequence)val);
		} else if (val instanceof Record) {
			StringBuffer sb = new StringBuffer(1024);
			JSONUtil.toJSON(val, sb);
			return sb.toString();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("json" + mm.getMessage("function.paramTypeError"));
		}
	}
}
