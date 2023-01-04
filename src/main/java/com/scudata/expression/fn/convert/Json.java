package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
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
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("json" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("json" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object val = param.getLeafExpression().calculate(ctx);
		if (val instanceof String) {
			if (option == null || option.indexOf('v') == -1) {
				char[] chars = ((String)val).toCharArray();
				return JSONUtil.parseJSON(chars, 0, chars.length - 1, option);
			} else {
				Expression exp = new Expression(cs, ctx, (String)val);
				return exp.calculate(ctx);
			}
		} else if (val instanceof Sequence) {
			return JSONUtil.toJSON((Sequence)val);
		} else if (val instanceof BaseRecord) {
			return JSONUtil.toJSON((BaseRecord)val);
		} else if (val == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("json" + mm.getMessage("function.paramTypeError"));
		}
	}
}
