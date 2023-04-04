package com.scudata.expression.mfn.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.mfn.sequence.FJoin;
import com.scudata.resources.EngineMessage;

/**
 * 针对A每一行计算w，再针对w计算x作为新字段F，T为w的别名可用在x中，x是~表示w本身，F若在A中则重新赋值
 * cs.fjoin(w:T,x:F,…;…)
 * @author RunQian
 *
 */
public class AttachFJoin extends OperableFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fjoin" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		Expression[] dimExps;
		String []aliasNames;
		Expression [][]newExps;
		String [][]newNames;
		
		if (param.getType() == IParam.Semicolon) {
			int count = param.getSubSize();
			dimExps = new Expression[count];
			aliasNames = new String[count];
			newExps = new Expression[count][];
			newNames = new String[count][];

			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fjoin" + mm.getMessage("function.invalidParam"));
				}

				FJoin.parseJoinItem(sub, i, dimExps, aliasNames, newExps, newNames, ctx);
			}
		} else {
			dimExps = new Expression[1];
			aliasNames = new String[1];
			newExps = new Expression[1][];
			newNames = new String[1][];

			FJoin.parseJoinItem(param, 0, dimExps, aliasNames, newExps, newNames, ctx);
		}
		
		return operable.fjoin(this, dimExps, aliasNames, newExps, newNames, option, ctx);
	}
}
