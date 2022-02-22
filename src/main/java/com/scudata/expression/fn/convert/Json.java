package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.JSONUtil;

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
			if (option == null || option.indexOf('v') == -1) {
				char[] chars = ((String)val).toCharArray();
				return JSONUtil.parseJSON(chars, 0, chars.length - 1);
			} else {
				Expression exp = new Expression(cs, ctx, (String)val);
				return exp.calculate(ctx);
			}
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
