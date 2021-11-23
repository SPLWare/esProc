package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 对参数执行逻辑或运算
 * cor(x1,…)
 * @author RunQian
 *
 */
public class Cor extends Function {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cor" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return Boolean.valueOf(Variant.isTrue(obj));
		} else {
			int size = param.getSubSize();
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("cor" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.isTrue(obj)) {
					return Boolean.TRUE;
				}
			}

			return Boolean.FALSE;
		}
	}
}
