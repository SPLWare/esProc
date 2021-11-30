package com.scudata.expression.mfn.pseudo;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.PseudoFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更新虚表数据
 * pseudo.update(P)
 * @author RunQian
 *
 */
public class Update extends PseudoFunction {
	public Object calculate(Context ctx) {
		String opt = option;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		boolean hasN = opt != null && opt.indexOf('n') != -1;
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			if (!hasN) {
				pseudo.update((Sequence)obj, opt);
				return pseudo;
			}
			return pseudo.update((Sequence)obj, opt);
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.paramTypeError"));
		} else {
			if (!hasN) {
				return pseudo;
			} else {
				return null;
			}
		}
	}
}
